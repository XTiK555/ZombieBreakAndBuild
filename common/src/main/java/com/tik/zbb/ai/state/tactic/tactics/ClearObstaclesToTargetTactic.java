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
    private final float MIN_STEP_MULTIPLIER = 0.01f;
    private final float MAX_STEP_MULTIPLIER = 1.0f;

    @Override
    public void execute(MobStateContext context)
    {
        double randomMultiplier = Mth.randomBetween(context.getMob().getRandom(), MIN_STEP_MULTIPLIER, MAX_STEP_MULTIPLIER);
        double checkDistance = MAX_CHECK_DISTANCE * randomMultiplier;

        Vec3 directionToTarget = context.getTarget().position().subtract(context.getMob().position()).normalize();
        Vec3 hitboxScanOffset = directionToTarget.scale(checkDistance);

        BlockPos blockToBreak = HitboxScanUtility.getNearestCollidingBlock(context.getLevel(), context.getMob(), hitboxScanOffset);

        if (blockToBreak != null)
        {
            context.getActionExecutor().tryExecuteBreakAction(blockToBreak);
        }
    }
}
