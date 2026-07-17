package com.tik.zbb.ai.state.states;

import com.tik.zbb.ai.state.BaseMobState;
import com.tik.zbb.ai.state.MobStateContext;
import com.tik.zbb.ai.state.Priority;
import com.tik.zbb.ai.state.tactic.IMobTactic;
import com.tik.zbb.ai.state.tactic.tactics.AdjustHeightToTargetTactic;
import com.tik.zbb.ai.state.tactic.tactics.BridgeToTargetTactic;
import com.tik.zbb.ai.state.tactic.tactics.ClearObstaclesToTargetTactic;
import com.tik.zbb.ai.state.tactic.tactics.MitigateDangerousBlocksTactic;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

public class BreakAndBuildState extends BaseMobState
{
    private final int STUCK_TICKS_TO_BREAKANDBUILD = 40;

    private final IMobTactic adjustHeightToTargetTactic = new AdjustHeightToTargetTactic();
    private final IMobTactic bridgeToTargetTactic = new BridgeToTargetTactic();
    private final IMobTactic clearObstaclesToTargetTactic = new ClearObstaclesToTargetTactic();
    private final IMobTactic mitigateDangerousBlocksTactic = new MitigateDangerousBlocksTactic();

    private int customStuckTicks;
    private Vec3 lastStuckCheckPos;

    public BreakAndBuildState()
    {
        mobTactics.add(adjustHeightToTargetTactic);
        mobTactics.add(bridgeToTargetTactic);
        mobTactics.add(clearObstaclesToTargetTactic);
        mobTactics.add(mitigateDangerousBlocksTactic);

        resetTransientState();
    }

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

        if (adjustHeightToTargetTactic.isRunning()) return Priority.High;
        if (navigation.isStuck() || isCustomStuck(context)) return Priority.High;
        if (path == null) return Priority.High;

        boolean hasActivePath = !path.isDone() && path.getNodeCount() > 0;
        boolean pathCanReachTarget = path.canReach();

        if (hasActivePath && pathCanReachTarget) return Priority.Low;

        Node endNode = path.getEndNode();

        if (endNode == null) return Priority.High;

        int breakBuildDistance = context.getConfigSnapshot().game().balance().pathEndBreakBuildDistance();
        double breakBuildDistanceSq = (double) breakBuildDistance * breakBuildDistance;
        double mobToEndNodeDistanceSq = context.getMob().distanceToSqr(endNode.x + 0.5D, endNode.y, endNode.z + 0.5D);

        boolean hasPartialPathAndMobReachedItsEnd = !pathCanReachTarget && mobToEndNodeDistanceSq < breakBuildDistanceSq;

        if (hasPartialPathAndMobReachedItsEnd) return Priority.High;

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

    @Override
    public void resetTransientState()
    {
        super.resetTransientState();

        customStuckTicks = 0;
        lastStuckCheckPos = null;
    }

    private boolean isCustomStuck(MobStateContext context)
    {
        var mob = context.getMob();
        var navigation = mob.getNavigation();
        var path = navigation.getPath();

        if (mob.getTarget() == null || path == null || path.isDone())
        {
            customStuckTicks = 0;
            lastStuckCheckPos = null;
            return false;
        }

        Vec3 currentPos = mob.position();

        if (lastStuckCheckPos == null)
        {
            lastStuckCheckPos = currentPos;
            customStuckTicks = 0;
            return false;
        }

        double movedSq = currentPos.distanceToSqr(lastStuckCheckPos);
        lastStuckCheckPos = currentPos;

        if (movedSq < 0.0009D)
        {
            customStuckTicks++;
        }
        else
        {
            customStuckTicks = 0;
        }

        return customStuckTicks >= STUCK_TICKS_TO_BREAKANDBUILD;
    }
}
