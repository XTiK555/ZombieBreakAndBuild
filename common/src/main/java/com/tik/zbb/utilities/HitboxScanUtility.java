package com.tik.zbb.utilities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

public class HitboxScanUtility
{
    public static BlockPos getNearestCollidingBlock(ServerLevel level, Mob mob, Vec3 offset)
    {
        List<BlockPos> collidingBlocks = findCollidingBlocks(level, mob, offset);
        if (collidingBlocks.isEmpty()) return null;

        BlockPos nearest = null;
        double bestDist = Double.MAX_VALUE;

        for (BlockPos collidingBlock : collidingBlocks)
        {
            double dist = collidingBlock.distSqr(mob.blockPosition());
            if (dist < bestDist)
            {
                bestDist = dist;
                nearest = collidingBlock;
            }
        }

        return nearest;
    }

    public static List<BlockPos> findCollidingBlocks(ServerLevel level, Mob mob, Vec3 offset)
    {
        AABB box = mob.getBoundingBox().move(offset).inflate(-0.01);

        int minX = Mth.floor(box.minX);
        int minY = Mth.floor(box.minY);
        int minZ = Mth.floor(box.minZ);
        int maxX = Mth.floor(box.maxX);
        int maxY = Mth.floor(box.maxY);
        int maxZ = Mth.floor(box.maxZ);

        List<BlockPos> result = new ArrayList<>();

        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
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

                    AABB localBox = box.move(-x, -y, -z);

                    boolean intersects = Shapes.joinIsNotEmpty(shape, Shapes.create(localBox), BooleanOp.AND);
                    if (intersects)
                    {
                        result.add(p.immutable());
                    }
                }
            }
        }

        return result;
    }
}
