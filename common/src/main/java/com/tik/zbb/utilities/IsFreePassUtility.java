package com.tik.zbb.utilities;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

public final class IsFreePassUtility
{
    public static boolean isFreePass(BlockPos pos, ServerLevel level)
    {
        BlockState blockState = level.getBlockState(pos);

        if (blockState.isAir()) return true;
        return blockState.getCollisionShape(level, pos).isEmpty();
    }
}
