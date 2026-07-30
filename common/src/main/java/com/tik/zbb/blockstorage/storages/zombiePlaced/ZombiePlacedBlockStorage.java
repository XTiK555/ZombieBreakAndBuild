package com.tik.zbb.blockstorage.storages.zombiePlaced;

import com.tik.zbb.blockstorage.BaseBlockStorage;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

public class ZombiePlacedBlockStorage extends BaseBlockStorage<BlockState, BlockState>
{
    @Override
    protected BlockState toStored(ServerLevel level, BlockState state)
    {
        return state;
    }

    @Override
    protected BlockState toData(BlockState state)
    {
        return state;
    }
}
