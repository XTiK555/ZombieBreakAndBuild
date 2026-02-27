package com.tik.zbb.utilities;

import net.minecraft.core.BlockPos;

public class GetHorizontalFrontBlockUtility
{
    public static BlockPos getPos(BlockPos from, BlockPos to)
    {
        final int deltaX = to.getX() - from.getX();
        final int deltaZ = to.getZ() - from.getZ();

        int dirX = 0, dirZ = 0;
        if (Math.abs(deltaX) > Math.abs(deltaZ)) dirX = Integer.signum(deltaX);
        else if (deltaZ != 0) dirZ = Integer.signum(deltaZ);

        return new BlockPos(from.getX() + dirX, from.getY(), from.getZ() + dirZ);
    }
}
