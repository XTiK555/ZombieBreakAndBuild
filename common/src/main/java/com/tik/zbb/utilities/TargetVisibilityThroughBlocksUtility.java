package com.tik.zbb.utilities;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

public final class TargetVisibilityThroughBlocksUtility
{
    private static final int AXIS_X = 1;
    private static final int AXIS_Y = 2;
    private static final int AXIS_Z = 4;

    private static final double TIE_TOLERANCE_ULPS = 8.0D;

    private TargetVisibilityThroughBlocksUtility()
    {
    }

    public static boolean canSeeThroughSolidBlocks(Mob mob, LivingEntity target, int maxSolidBlocks)
    {
        if (maxSolidBlocks == 0)
        {
            return true;
        }

        Level level = mob.level();

        if (level != target.level())
        {
            return false;
        }

        double fromX = mob.getX();
        double fromY = mob.getEyeY();
        double fromZ = mob.getZ();

        double toX = target.getX();
        double toY = target.getEyeY();
        double toZ = target.getZ();

        int x = Mth.floor(fromX);
        int y = Mth.floor(fromY);
        int z = Mth.floor(fromZ);

        int endX = Mth.floor(toX);
        int endY = Mth.floor(toY);
        int endZ = Mth.floor(toZ);

        if (x == endX && y == endY && z == endZ)
        {
            return true;
        }

        double deltaX = toX - fromX;
        double deltaY = toY - fromY;
        double deltaZ = toZ - fromZ;

        int stepX = Integer.compare(endX, x);
        int stepY = Integer.compare(endY, y);
        int stepZ = Integer.compare(endZ, z);

        double tDeltaX = stepX == 0
                ? Double.POSITIVE_INFINITY
                : 1.0D / Math.abs(deltaX);

        double tDeltaY = stepY == 0
                ? Double.POSITIVE_INFINITY
                : 1.0D / Math.abs(deltaY);

        double tDeltaZ = stepZ == 0
                ? Double.POSITIVE_INFINITY
                : 1.0D / Math.abs(deltaZ);

        double tMaxX = firstBoundaryIntersection(
                fromX,
                x,
                stepX,
                deltaX
        );

        double tMaxY = firstBoundaryIntersection(
                fromY,
                y,
                stepY,
                deltaY
        );

        double tMaxZ = firstBoundaryIntersection(
                fromZ,
                z,
                stepZ,
                deltaZ
        );

        int stationaryBoundaryMask = 0;

        if (deltaX == 0.0D && fromX == x)
        {
            stationaryBoundaryMask |= AXIS_X;
        }

        if (deltaY == 0.0D && fromY == y)
        {
            stationaryBoundaryMask |= AXIS_Y;
        }

        if (deltaZ == 0.0D && fromZ == z)
        {
            stationaryBoundaryMask |= AXIS_Z;
        }

        int solidBlocks = 0;

        BlockPos.MutableBlockPos mutablePos =
                new BlockPos.MutableBlockPos();

        while (x != endX || y != endY || z != endZ)
        {
            double nextIntersection =
                    Math.min(tMaxX, Math.min(tMaxY, tMaxZ));

            double tolerance =
                    Math.ulp(nextIntersection) * TIE_TOLERANCE_ULPS;

            int crossedAxes = 0;

            if (tMaxX <= nextIntersection + tolerance)
            {
                crossedAxes |= AXIS_X;
            }

            if (tMaxY <= nextIntersection + tolerance)
            {
                crossedAxes |= AXIS_Y;
            }

            if (tMaxZ <= nextIntersection + tolerance)
            {
                crossedAxes |= AXIS_Z;
            }

            if ((crossedAxes & (crossedAxes - 1)) == 0)
            {
                if (crossedAxes == AXIS_X)
                {
                    x += stepX;
                    tMaxX += tDeltaX;
                }
                else if (crossedAxes == AXIS_Y)
                {
                    y += stepY;
                    tMaxY += tDeltaY;
                }
                else
                {
                    z += stepZ;
                    tMaxZ += tDeltaZ;
                }

                solidBlocks += countSolidVariants(
                        level,
                        mutablePos,
                        x,
                        y,
                        z,
                        endX,
                        endY,
                        endZ,
                        stationaryBoundaryMask
                );

                if (solidBlocks > maxSolidBlocks)
                {
                    return false;
                }

                continue;
            }

            for (
                    int subset = crossedAxes;
                    subset != 0;
                    subset = (subset - 1) & crossedAxes
            )
            {
                int testX = x +
                        ((subset & AXIS_X) != 0 ? stepX : 0);

                int testY = y +
                        ((subset & AXIS_Y) != 0 ? stepY : 0);

                int testZ = z +
                        ((subset & AXIS_Z) != 0 ? stepZ : 0);

                solidBlocks += countSolidVariants(
                        level,
                        mutablePos,
                        testX,
                        testY,
                        testZ,
                        endX,
                        endY,
                        endZ,
                        stationaryBoundaryMask
                );

                if (solidBlocks > maxSolidBlocks)
                {
                    return false;
                }
            }

            if ((crossedAxes & AXIS_X) != 0)
            {
                x += stepX;
                tMaxX += tDeltaX;
            }

            if ((crossedAxes & AXIS_Y) != 0)
            {
                y += stepY;
                tMaxY += tDeltaY;
            }

            if ((crossedAxes & AXIS_Z) != 0)
            {
                z += stepZ;
                tMaxZ += tDeltaZ;
            }
        }

        return true;
    }

    private static int countSolidVariants(
            Level level,
            BlockPos.MutableBlockPos mutablePos,
            int x,
            int y,
            int z,
            int endX,
            int endY,
            int endZ,
            int stationaryBoundaryMask
    )
    {
        int count = 0;

        if (!isEndPosition(x, y, z, endX, endY, endZ) &&
                isSolidBlock(level, mutablePos, x, y, z))
        {
            count++;
        }

        for (
                int subset = stationaryBoundaryMask;
                subset != 0;
                subset = (subset - 1) & stationaryBoundaryMask
        )
        {
            int shiftedX = x -
                    ((subset & AXIS_X) != 0 ? 1 : 0);

            int shiftedY = y -
                    ((subset & AXIS_Y) != 0 ? 1 : 0);

            int shiftedZ = z -
                    ((subset & AXIS_Z) != 0 ? 1 : 0);

            if (!isEndPosition(
                    shiftedX,
                    shiftedY,
                    shiftedZ,
                    endX,
                    endY,
                    endZ
            ) &&
                    isSolidBlock(
                            level,
                            mutablePos,
                            shiftedX,
                            shiftedY,
                            shiftedZ
                    ))
            {
                count++;
            }
        }

        return count;
    }

    private static boolean isSolidBlock(
            Level level,
            BlockPos.MutableBlockPos mutablePos,
            int x,
            int y,
            int z
    )
    {
        mutablePos.set(x, y, z);

        return level.getBlockState(mutablePos).isSolidRender();
    }

    private static boolean isEndPosition(
            int x,
            int y,
            int z,
            int endX,
            int endY,
            int endZ
    )
    {
        return x == endX && y == endY && z == endZ;
    }

    private static double firstBoundaryIntersection(
            double coordinate,
            int blockCoordinate,
            int step,
            double delta
    )
    {
        if (step == 0)
        {
            return Double.POSITIVE_INFINITY;
        }

        double nextBoundary = step > 0
                ? blockCoordinate + 1.0D
                : blockCoordinate;

        return (nextBoundary - coordinate) / delta;
    }
}