package com.tik.zbb.blockstorage.storages.buildDisappear;

import com.tik.zbb.blockstorage.BaseBlockStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class BuildDisappearBlockStorage extends BaseBlockStorage<BuildDisappearEntry>
{
    public void addBuildDisappearData(ServerLevel level, BlockPos pos)
    {
        BlockState state = level.getBlockState(pos);

        put(level, pos, new BuildDisappearEntry(state, level.getGameTime()));
    }

    @Override
    protected boolean isExpired(BuildDisappearEntry entry, long now, long ttlTicks)
    {
        return now - entry.tick() > ttlTicks;
    }

    @Override
    protected void onRemove(ServerLevel level, long posKey, BuildDisappearEntry entry)
    {
        BlockPos pos = BlockPos.of(posKey);
        BlockState currentState = level.getBlockState(pos);

        if (currentState.equals(entry.state()))
        {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
    }
}
