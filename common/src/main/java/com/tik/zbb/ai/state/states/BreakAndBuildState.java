package com.tik.zbb.ai.state.states;

import com.tik.zbb.ai.state.IMobState;
import com.tik.zbb.ai.state.MobStateContext;
import com.tik.zbb.ai.state.Priority;
import com.tik.zbb.ai.state.tactic.IMobTactic;
import com.tik.zbb.ai.state.tactic.tactics.*;
import com.tik.zbb.utilities.DistanceMultiplierUtility;
import com.tik.zbb.utilities.SecondsToTicksUtility;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;

public class BreakAndBuildState implements IMobState
{
    private final IMobTactic adjustHeightToTargetTactic = new AdjustHeightToTargetTactic();
    private final IMobTactic bridgeToTargetTactic = new BridgeToTargetTactic();
    private final IMobTactic clearObstaclesToTargetTactic = new ClearObstaclesToTargetTactic();
    private final IMobTactic goToTargetTactic = new GoToTargetTactic();
    private final IMobTactic mitigateDangerousBlocksTactic = new MitigateDangerousBlocksTactic();

    private Priority lastPriority = Priority.Low;
    private int stuckTicks;
    private double lastTargetDistSq = Double.NaN;
    private LivingEntity lastTarget = null;

    @Override
    public void tick(MobStateContext context)
    {
        adjustHeightToTargetTactic.execute(context);
        clearObstaclesToTargetTactic.execute(context);
        if (!adjustHeightToTargetTactic.isRunning())
        {
            bridgeToTargetTactic.execute(context);
            mitigateDangerousBlocksTactic.execute(context);
        }
        goToTargetTactic.execute(context);
    }

    @Override
    public Priority calculatePriority(MobStateContext context)
    {
        long now = context.getMob().level().getGameTime();

        updateStuckTicks(context, now);

        if (context.getAiTimers().checkPathCooldownPassed(now))
        {
            double pathCheckSeconds = DistanceMultiplierUtility.applyDistanceMultiplier(
                    context.getConfigSnapshot().data().balance.optimization.pathCheckInterval,
                    context.getMob().distanceTo(context.getTarget()),
                    context.getConfigSnapshot().data().balance.optimization.distanceScaleStartBlocks,
                    context.getConfigSnapshot().data().balance.optimization.distanceScaleMaxBlocks,
                    context.getConfigSnapshot().data().balance.optimization.distanceScaleMaxMultiplier);
            context.getAiTimers().setCheckPathCooldownUntil(now + SecondsToTicksUtility.toTicks(pathCheckSeconds, 1));

            PathNavigation nav = context.getMob().getNavigation();
            Path path = nav.getPath();

            if (path == null)
            {
                lastPriority = Priority.High;
                return Priority.High;
            }

            boolean hasActivePath = !path.isDone() && path.getNodeCount() > 0;
            boolean isStuckTooLong = stuckTicks >= SecondsToTicksUtility.toTicks(context.getConfigSnapshot().data().balance.stuckSecondsBeforeBreakAndBuild, 1);

            if (hasActivePath && !isStuckTooLong)
            {
                Node endNode = path.getEndNode();

                if (endNode != null)
                {
                    double endNodeDistanceSq = context.getMob().distanceToSqr(endNode.x + 0.5, endNode.y, endNode.z + 0.5);

                    if (endNodeDistanceSq > 2 * 2)
                    {
                        lastPriority = Priority.Low;
                        return Priority.Low;
                    }
                }
                else
                {
                    lastPriority = Priority.Low;
                    return Priority.Low;
                }
            }

            lastPriority = Priority.High;
            return Priority.High;
        }
        else
        {
            return lastPriority;
        }
    }

    private void updateStuckTicks(MobStateContext context, long now)
    {
        if (context.getTarget() != lastTarget)
        {
            lastTarget = context.getTarget();
            lastTargetDistSq = Double.NaN;
            stuckTicks = 0;
        }

        double distSq = context.getMob().distanceToSqr(context.getTarget());
        if (Double.isNaN(lastTargetDistSq)) lastTargetDistSq = distSq;

        if (context.getAiTimers().freezePassed(now))
        {
            if (distSq < lastTargetDistSq - 0.5)
            {
                stuckTicks = 0;
            }
            else
            {
                stuckTicks++;
            }
            lastTargetDistSq = distSq;
        }
    }
}
