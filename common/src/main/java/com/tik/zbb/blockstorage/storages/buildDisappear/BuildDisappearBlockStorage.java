package com.tik.zbb.blockstorage.storages.buildDisappear;

import com.tik.zbb.ai.action.actions.build.BuildAction;
import com.tik.zbb.blockstorage.BaseBlockStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.greenrobot.eventbus.Subscribe;

public class BuildDisappearBlockStorage extends BaseBlockStorage<BuildDisappearBlockStorageEntry>
{
    @Subscribe
    public void onAnyBlockPlaced(BuildAction.OnAnyBlockPlacedEvent event)
    {
        if (!event.configSnapshot().data().blockReturning.builtBlocksDisappearing) return;

        addBuildDisappearData(event.level(), event.pos());
    }

    public void addBuildDisappearData(ServerLevel level, BlockPos pos)
    {
        BlockState state = level.getBlockState(pos);

        put(level, pos, new BuildDisappearBlockStorageEntry(state, level.getGameTime()));
    }

    @Override
    protected boolean isExpired(BuildDisappearBlockStorageEntry entry, long now, long ttlTicks)
    {
        return now - entry.tick() > ttlTicks;
    }

    @Override
    protected void onRemove(ServerLevel level, long posKey, BuildDisappearBlockStorageEntry entry)
    {
        BlockPos pos = BlockPos.of(posKey);
        BlockState currentState = level.getBlockState(pos);

        if (currentState.equals(entry.state()))
        {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
    }
}
