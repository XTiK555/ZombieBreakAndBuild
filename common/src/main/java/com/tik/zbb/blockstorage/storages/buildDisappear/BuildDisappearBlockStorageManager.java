package com.tik.zbb.blockstorage.storages.buildDisappear;

import com.tik.zbb.Constants;
import com.tik.zbb.ai.action.actions.build.BuildAction;
import com.tik.zbb.event.MixinEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.greenrobot.eventbus.Subscribe;

public class BuildDisappearBlockStorageManager
{
    public record OnBuildBlockDisappearEvent(ServerLevel level, BlockPos pos, BlockState oldState) {}

    private final BuildDisappearBlockStorage buildDisappearBlockStorage = new BuildDisappearBlockStorage();

    @Subscribe
    public void onAnyBlockPlaced(BuildAction.OnAnyBlockPlacedEvent event)
    {
        if (!event.configSnapshot().data().blockRestoration.builtBlocksDisappearing) return;

        buildDisappearBlockStorage.put(event.level(), event.pos(), new BuildDisappearBlockStorageEntry(event.level().getBlockState(event.pos()), event.level().getGameTime()));
    }

    @Subscribe
    public void onLevelChunkBlockChanged(MixinEvents.OnLevelChunkBlockChangedEvent event)
    {
        buildDisappearBlockStorage.remove(event.level(), event.pos());
    }

    @Subscribe
    public void onBuildDisappearBlockStorageRemove(BuildDisappearBlockStorage.OnRemovedEvent event)
    {
        BlockState currentState = event.level().getBlockState(event.pos());

        if (currentState.is(event.entry().state().getBlock()))
        {
            event.level().setBlockAndUpdate(event.pos(), Blocks.AIR.defaultBlockState());

            Constants.EVENT_BUS.post(new OnBuildBlockDisappearEvent(event.level(), event.pos(), event.entry().state()));
        }
    }

    public boolean contains(ServerLevel level, BlockPos pos)
    {
        return buildDisappearBlockStorage.contains(level, pos);
    }

    public void cleanup(ServerLevel level, long ttlTicks)
    {
        buildDisappearBlockStorage.cleanup(level, ttlTicks);
    }
}
