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

        double distance = from.distanceTo(to);
        if (distance <= 0.0001D)
        {
            return true;
        }

        int steps = Math.max(1, (int) Math.ceil(distance * 5.0D));
        int solidBlocks = 0;

        BlockPos startPos = BlockPos.containing(from);
        BlockPos endPos = BlockPos.containing(to);
        BlockPos lastPos = null;

        for (int i = 1; i < steps; i++)
        {
            double t = (double) i / (double) steps;
            Vec3 point = from.lerp(to, t);
            BlockPos pos = BlockPos.containing(point);

            if (pos.equals(lastPos))
            {
                continue;
            }
            lastPos = pos;

            if (pos.equals(startPos) || pos.equals(endPos))
            {
                continue;
            }

            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && state.isSolidRender())
            {
                solidBlocks++;
                if (solidBlocks > maxSolidBlocks)
                {
                    return false;
                }
            }
        }

        return true;
    }
}
