package com.tik.zbb.ai.state.tactic.tactics;

import com.tik.zbb.ai.state.MobStateContext;
import com.tik.zbb.ai.state.tactic.IMobTactic;
import com.tik.zbb.utilities.HitboxScanUtility;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class ClearObstaclesToTargetTactic implements IMobTactic
{
    private final double MAX_CHECK_DISTANCE = 0.9;

    @Override
    public void execute(MobStateContext context)
    {
        double randomMultiplier = Mth.randomBetween(context.getMob().getRandom(), 0.01f, 1);
        double randomizedStep = MAX_CHECK_DISTANCE * randomMultiplier;

        Vec3 directionToTarget = context.getTarget().position().subtract(context.getMob().position()).normalize();
        Vec3 hitboxScanOffset = directionToTarget.scale(randomizedStep);

        BlockPos blockToBreak = HitboxScanUtility.getNearestCollidingBlock(context.getLevel(), context.getMob(), hitboxScanOffset);

        if (blockToBreak != null)
        {
            context.getActionExecutor().tryExecuteBreakAction(blockToBreak);
        }
    }
}
