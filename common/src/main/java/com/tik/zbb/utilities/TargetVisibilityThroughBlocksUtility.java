package com.tik.zbb.utilities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class TargetVisibilityThroughBlocksUtility
{
    public static boolean canSeeThroughSolidBlocks(Mob mob, LivingEntity target, int maxSolidBlocks)
    {
        if (maxSolidBlocks == 0)
        {
            return true;
        }

        Level level = mob.level();
        Vec3 from = mob.getEyePosition();
        Vec3 to = target.getEyePosition();

        BlockPos startPos = BlockPos.containing(from);
        BlockPos endPos = BlockPos.containing(to);

        int x = startPos.getX();
        int y = startPos.getY();
        int z = startPos.getZ();
        int endX = endPos.getX();
        int endY = endPos.getY();
        int endZ = endPos.getZ();

        double deltaX = to.x - from.x;
        double deltaY = to.y - from.y;
        double deltaZ = to.z - from.z;

        int stepX = Integer.compare(endX, x);
        int stepY = Integer.compare(endY, y);
        int stepZ = Integer.compare(endZ, z);

        double tDeltaX = reciprocalAbsolute(deltaX);
        double tDeltaY = reciprocalAbsolute(deltaY);
        double tDeltaZ = reciprocalAbsolute(deltaZ);
        double tMaxX = distanceToNextBoundary(from.x, x, stepX, deltaX);
        double tMaxY = distanceToNextBoundary(from.y, y, stepY, deltaY);
        double tMaxZ = distanceToNextBoundary(from.z, z, stepZ, deltaZ);

        int solidBlocks = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, y, z);

        while (x != endX || y != endY || z != endZ)
        {
            double nextIntersection = Math.min(tMaxX, Math.min(tMaxY, tMaxZ));

            if (tMaxX <= nextIntersection)
            {
                x += stepX;
                tMaxX += tDeltaX;
            }
            if (tMaxY <= nextIntersection)
            {
                y += stepY;
                tMaxY += tDeltaY;
            }
            if (tMaxZ <= nextIntersection)
            {
                z += stepZ;
                tMaxZ += tDeltaZ;
            }

            if (x == endX && y == endY && z == endZ)
            {
                break;
            }

            pos.set(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && state.isSolidRender() && ++solidBlocks > maxSolidBlocks)
            {
                return false;
            }
        }

        return true;
    }

    private static double reciprocalAbsolute(double delta)
    {
        return delta == 0.0D ? Double.POSITIVE_INFINITY : 1.0D / Math.abs(delta);
    }

    private static double distanceToNextBoundary(double coordinate, int blockCoordinate, int step, double delta)
    {
        if (step == 0)
        {
            return Double.POSITIVE_INFINITY;
        }

        double nextBoundary = step > 0 ? blockCoordinate + 1.0D : blockCoordinate;
        return (nextBoundary - coordinate) / delta;
    }
}
