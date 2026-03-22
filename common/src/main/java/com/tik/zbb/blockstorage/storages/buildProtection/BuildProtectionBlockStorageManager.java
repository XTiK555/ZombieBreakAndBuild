package com.tik.zbb.blockstorage.storages.buildProtection;

import com.tik.zbb.ai.action.actions.build.BuildAction;
import com.tik.zbb.event.MixinEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.greenrobot.eventbus.Subscribe;

public class BuildProtectionBlockStorageManager
{
    private final BuildProtectionBlockStorage buildProtectionBlockStorage = new BuildProtectionBlockStorage();

    @Subscribe
    public void onAnyBlockPlaced(BuildAction.OnAnyBlockPlacedEvent event)
    {
        buildProtectionBlockStorage.put(event.level(), event.pos(), new BuildProtectionBlockStorageEntry(event.level().getGameTime()));
    }

    @Subscribe
    public void onLevelChunkBlockChanged(MixinEvents.OnLevelChunkBlockChangedEvent event)
    {
        buildProtectionBlockStorage.remove(event.level(), event.pos());
    }

    public boolean contains(ServerLevel level, BlockPos pos)
    {
        return buildProtectionBlockStorage.contains(level, pos);
    }

    public void cleanup(ServerLevel level, long ttlTicks)
    {
        buildProtectionBlockStorage.cleanup(level, ttlTicks);
    }
}
