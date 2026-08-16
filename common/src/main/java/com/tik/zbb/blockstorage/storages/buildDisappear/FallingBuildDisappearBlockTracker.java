package com.tik.zbb.blockstorage.storages.buildDisappear;

import com.tik.zbb.MainCommon;
import com.tik.zbb.blockstorage.BlockStorages;
import com.tik.zbb.event.MixinEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.greenrobot.eventbus.Subscribe;

import java.util.HashMap;
import java.util.Map;

public final class FallingBuildDisappearBlockTracker
{
    private final BuildDisappearBlockStorageManager storage;
    private final Map<FallOrigin, BuildDisappearBlockStorageEntry> entriesInFlight = new HashMap<>();

    public FallingBuildDisappearBlockTracker()
    {
        this.storage = BlockStorages.BUILD_DISAPPEAR_MANAGER;
    }

    @Subscribe
    public void onFallingBlockStarted(MixinEvents.OnFallingBlockStartedEvent event)
    {
        BuildDisappearBlockStorageEntry entry = storage.get(event.level(), event.startPos());
        if (entry == null || !entry.placedState().is(event.blockState().getBlock())) return;

        entry = storage.discard(event.level(), event.startPos());
        if (entry != null) entriesInFlight.put(new FallOrigin(event.level(), event.startPos()), entry);
    }

    @Subscribe
    public void onFallingBlockFinished(MixinEvents.OnFallingBlockFinishedEvent event)
    {
        BuildDisappearBlockStorageEntry entry = entriesInFlight.remove(new FallOrigin(event.level(), event.startPos()));
        if (entry == null) return;

        if (event.level().getBlockState(event.finalPos()).is(event.blockState().getBlock()))
        {
            storage.put(event.level(), event.finalPos(), entry);
        }
    }

    @Subscribe
    public void onServerStopping(MainCommon.OnServerStoppingEvent event)
    {
        entriesInFlight.clear();
    }

    private record FallOrigin(ServerLevel level, BlockPos pos) {}
}
