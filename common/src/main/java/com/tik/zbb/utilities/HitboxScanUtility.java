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

import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class HitboxScanUtility
{
    public static BlockPos getNearestCollidingBlockWithHitbox(ServerLevel level, Mob mob, Vec3 offset)
    {
        AABB box = mob.getBoundingBox().move(offset).inflate(-0.01);

        int minX = Mth.floor(box.minX);
        int minY = Mth.floor(box.minY);
        int minZ = Mth.floor(box.minZ);
        int maxX = Mth.floor(box.maxX);
        int maxY = Mth.floor(box.maxY);
        int maxZ = Mth.floor(box.maxZ);

        VoxelShape boxShape = Shapes.create(box);

        BlockPos nearest = null;
        double bestDist = Double.MAX_VALUE;

        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        BlockPos mobPos = mob.blockPosition();

        for (int x = minX; x <= maxX; x++)
        {
            for (int y = minY; y <= maxY; y++)
            {
                for (int z = minZ; z <= maxZ; z++)
                {
                    p.set(x, y, z);

                    BlockState state = level.getBlockState(p);
                    if (state.isAir()) continue;

                    VoxelShape shape = state.getCollisionShape(level, p);
                    if (shape.isEmpty()) continue;

                    if (!shape.bounds().move(x, y, z).intersects(box)) continue;

                    if (Shapes.joinIsNotEmpty(shape.move(x, y, z), boxShape, BooleanOp.AND))
                    {
                        double dist = p.distSqr(mobPos);
                        if (dist < bestDist)
                        {
                            bestDist = dist;
                            nearest = p.immutable();
                        }
                    }
                }
            }
        }

        return nearest;
    }

    public static BlockPos findNearestBlockInInflatedHitbox(ServerLevel level, Mob mob, double inflate, Predicate<BlockState> predicate)
    {
        BlockPos mobPos = mob.blockPosition();

        final BlockPos[] nearest = {null};
        final double[] bestDist = {Double.MAX_VALUE};

        forEachBlockStateInInflatedHitbox(level, mob, inflate, (pos, state) ->
        {
            if (!predicate.test(state)) return;

            double dist = pos.distSqr(mobPos);
            if (dist < bestDist[0])
            {
                bestDist[0] = dist;
                nearest[0] = pos;
            }
        });

        return nearest[0];
    }

    public static void forEachBlockStateInInflatedHitbox(ServerLevel level, Mob mob, double inflate, BiConsumer<BlockPos, BlockState> consumer)
    {
        AABB box = mob.getBoundingBox().inflate(inflate);

        int minX = Mth.floor(box.minX);
        int minY = Mth.floor(box.minY);
        int minZ = Mth.floor(box.minZ);
        int maxX = Mth.floor(box.maxX);
        int maxY = Mth.floor(box.maxY);
        int maxZ = Mth.floor(box.maxZ);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = minX; x <= maxX; x++)
        {
            for (int y = minY; y <= maxY; y++)
            {
                for (int z = minZ; z <= maxZ; z++)
                {
                    pos.set(x, y, z);
                    consumer.accept(pos.immutable(), level.getBlockState(pos));
                }
            }
        }
    }
}
