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
        AABB hitbox = mob.getBoundingBox().move(offset);
        return findNearestCollidingBlock(level, hitbox, mob.position(), state -> !state.isAir());
    }

    public static BlockPos findNearestBlockInHitbox(ServerLevel level, Mob mob, double inflate, Predicate<BlockState> predicate)
    {
        AABB scanBox = mob.getBoundingBox().inflate(inflate);
        return findNearestMatchingBlock(level, scanBox, mob.position(), (currentLevel, pos, state) -> predicate.test(state));
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

            if (!collisionShape.bounds().move(pos.getX(), pos.getY(), pos.getZ()).intersects(hitbox))
            {
                return false;
            }

            return Shapes.joinIsNotEmpty(collisionShape.move(pos.getX(), pos.getY(), pos.getZ()), hitboxShape, BooleanOp.AND);
        });
    }

    private static BlockPos findNearestMatchingBlock(ServerLevel level, AABB scanBox, Vec3 origin, BlockMatcher matcher)
    {
        BlockPos nearest = null;
        double bestDist = Double.MAX_VALUE;

        int minX = Mth.floor(scanBox.minX);
        int minY = Mth.floor(scanBox.minY);
        int minZ = Mth.floor(scanBox.minZ);
        int maxX = Mth.floor(scanBox.maxX - 1.0E-7D);
        int maxY = Mth.floor(scanBox.maxY - 1.0E-7D);
        int maxZ = Mth.floor(scanBox.maxZ - 1.0E-7D);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = minX; x <= maxX; x++)
        {
            for (int y = minY; y <= maxY; y++)
            {
                for (int z = minZ; z <= maxZ; z++)
                {
                    pos.set(x, y, z);

                    BlockState state = level.getBlockState(pos);
                    if (!matcher.matches(level, pos, state))
                    {
                        continue;
                    }

                    double dist = Vec3.atCenterOf(pos).distanceToSqr(origin);
                    if (dist < bestDist)
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