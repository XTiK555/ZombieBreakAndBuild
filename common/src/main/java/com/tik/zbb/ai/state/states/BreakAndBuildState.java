package com.tik.zbb.ai.state.states;

import com.tik.zbb.ai.state.IMobState;
import com.tik.zbb.ai.state.MobStateContext;
import com.tik.zbb.ai.state.Priority;
import com.tik.zbb.ai.state.tactic.IMobTactic;
import com.tik.zbb.ai.state.tactic.tactics.*;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;

public class BreakAndBuildState implements IMobState
{
    private final IMobTactic adjustHeightToTargetTactic = new AdjustHeightToTargetTactic();
    private final IMobTactic bridgeToTargetTactic = new BridgeToTargetTactic();
    private final IMobTactic clearObstaclesToTargetTactic = new ClearObstaclesToTargetTactic();
    private final IMobTactic mitigateDangerousBlocksTactic = new MitigateDangerousBlocksTactic();

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
    }

    @Override
    public Priority calculatePriority(MobStateContext context)
    {
        PathNavigation navigation = context.getMob().getNavigation();
        Path path = navigation.getPath();

        if (adjustHeightToTargetTactic.isRunning())
        {
            return Priority.High;
        }

        if (navigation.isStuck())
        {
            return Priority.High;
        }

        if (path == null)
        {
            return Priority.High;
        }

        boolean hasActivePath = !path.isDone() && path.getNodeCount() > 0;
        boolean pathCanReachTarget = path.canReach();

        if (hasActivePath && pathCanReachTarget)
        {
            return Priority.Low;
        }

        Node endNode = path.getEndNode();
        if (endNode == null)
        {
            return Priority.High;
        }

        double breakBuildDistanceSq = context.getConfigSnapshot().data().balance.endNodeBreakBuildDistance * context.getConfigSnapshot().data().balance.endNodeBreakBuildDistance;
        double mobToEndNodeDistanceSq = context.getMob().distanceToSqr(endNode.x + 0.5D, endNode.y, endNode.z + 0.5D);

        boolean hasPartialPathAndMobReachedItsEnd = !pathCanReachTarget && mobToEndNodeDistanceSq < breakBuildDistanceSq;
        if (hasPartialPathAndMobReachedItsEnd)
        {
            return Priority.High;
        }

        if (path.isDone())
        {
            double mobToTargetDistanceSq = context.getMob().distanceToSqr(context.getTarget());

            if (mobToTargetDistanceSq > breakBuildDistanceSq)
            {
                return Priority.High;
            }
        }

        return Priority.Low;
    }
}
