package com.tik.zbb.utilities;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.Predicate;

public final class HitboxScanUtility
{
    public static BlockPos getNearestCollidingBlockWithHitbox(ServerLevel level, Mob mob, Vec3 offset)
    {
        AABB mobHitbox = mob.getBoundingBox();
        AABB scanHitbox = mobHitbox.move(offset);
        return findNearestCollidingBlock(level, scanHitbox, mobHitbox.getCenter(), state -> !state.isAir());
    }

    public static BlockPos findNearestBlockInHitbox(ServerLevel level, Mob mob, double inflate, Predicate<BlockState> predicate)
    {
        AABB scanBox = mob.getBoundingBox().inflate(inflate);
        return findNearestMatchingBlock(level, scanBox, mob.getBoundingBox().getCenter(), (currentLevel, pos, state) -> predicate.test(state));
    }

    private static BlockPos findNearestCollidingBlock(ServerLevel level, AABB hitbox, Vec3 origin, Predicate<BlockState> predicate)
    {
        VoxelShape hitboxShape = Shapes.create(hitbox);

        return findNearestMatchingBlock(level, hitbox, origin, (currentLevel, pos, state) ->
        {
            if (!predicate.test(state))
            {
                return false;
            }

            VoxelShape collisionShape = state.getCollisionShape(currentLevel, pos);
            if (collisionShape.isEmpty())
            {
                return false;
            }

            AABB bounds = collisionShape.bounds();
            int posX = pos.getX();
            int posY = pos.getY();
            int posZ = pos.getZ();

            if (hitbox.minX >= bounds.maxX + posX || hitbox.maxX <= bounds.minX + posX
                    || hitbox.minY >= bounds.maxY + posY || hitbox.maxY <= bounds.minY + posY
                    || hitbox.minZ >= bounds.maxZ + posZ || hitbox.maxZ <= bounds.minZ + posZ)
            {
                return false;
            }

            return Shapes.joinIsNotEmpty(collisionShape.move(posX, posY, posZ), hitboxShape, BooleanOp.AND);
        });
    }

    private static BlockPos findNearestMatchingBlock(ServerLevel level, AABB scanBox, Vec3 origin, BlockMatcher matcher)
    {
        BlockPos nearest = null;
        double bestDist = Double.MAX_VALUE;

        double originX = origin.x;
        double originY = origin.y;
        double originZ = origin.z;

        int minX = Mth.floor(scanBox.minX);
        int minY = Mth.floor(scanBox.minY);
        int minZ = Mth.floor(scanBox.minZ);
        int maxX = Mth.floor(scanBox.maxX - 1.0E-7D);
        int maxY = Mth.floor(scanBox.maxY - 1.0E-7D);
        int maxZ = Mth.floor(scanBox.maxZ - 1.0E-7D);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = minX; x <= maxX; x++)
        {
            double dx = (x + 0.5D) - originX;
            double dxSq = dx * dx;
            if (dxSq >= bestDist)
            {
                continue;
            }

            for (int y = minY; y <= maxY; y++)
            {
                double dy = (y + 0.5D) - originY;
                double dxySq = dxSq + dy * dy;
                if (dxySq >= bestDist)
                {
                    continue;
                }

                for (int z = minZ; z <= maxZ; z++)
                {
                    double dz = (z + 0.5D) - originZ;
                    double dist = dxySq + dz * dz;
                    if (dist >= bestDist)
                    {
                        continue;
                    }

                    pos.set(x, y, z);

                    BlockState state = level.getBlockState(pos);
                    if (matcher.matches(level, pos, state))
                    {
                        bestDist = dist;
                        nearest = pos.immutable();
                    }
                }
            }
        }

        return nearest;
    }

    @FunctionalInterface
    private interface BlockMatcher
    {
        boolean matches(ServerLevel level, BlockPos pos, BlockState state);
    }
}
