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
    private static final int STUCK_TICKS_TO_BREAK_AND_BUILD = 40;
    private static final double STUCK_RADIUS = 1D;
    private static final int MAX_NO_PATH_TICKS = 10;

    private final IMobTactic adjustHeightToTargetTactic = new AdjustHeightToTargetTactic();
    private final IMobTactic bridgeToTargetTactic = new BridgeToTargetTactic();
    private final IMobTactic clearObstaclesToTargetTactic = new ClearObstaclesToTargetTactic();
    private final IMobTactic mitigateDangerousBlocksTactic = new MitigateDangerousBlocksTactic();

    private final HardStuckDetector hardStuckDetector = new HardStuckDetector(STUCK_RADIUS, STUCK_TICKS_TO_BREAK_AND_BUILD);

    private int noPathTicks;

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
        if (isPathNullForLong(path)) return Priority.High;
        if (path == null) return Priority.Low;

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

        hardStuckDetector.reset();
    }

    private boolean isPathNullForLong(Path path)
    {
        if (path == null)
        {
            if (noPathTicks >= MAX_NO_PATH_TICKS)
            {
                return true;
            }
            else
            {
                noPathTicks++;
            }
        }
        else
        {
            noPathTicks = 0;
        }

        return false;
    }

    private boolean isCustomStuck(MobStateContext context)
    {
        var mob = context.getMob();
        var navigation = mob.getNavigation();
        var path = navigation.getPath();

        if (mob.getTarget() == null || path == null || path.isDone())
        {
            hardStuckDetector.reset();
            return false;
        }

        return hardStuckDetector.update(mob.position(), mob.level().getGameTime());
    }

    private static final class HardStuckDetector
    {
        private final double radiusSquared;
        private final long stuckTicks;

        private Vec3 anchor;
        private long anchoredAtTick;

        HardStuckDetector(double radius, long stuckTicks)
        {
            this.radiusSquared = radius * radius;
            this.stuckTicks = stuckTicks;
        }

        boolean update(Vec3 position, long gameTime)
        {
            if (anchor == null || gameTime < anchoredAtTick || position.distanceToSqr(anchor) > radiusSquared)
            {
                anchor = position;
                anchoredAtTick = gameTime;
                return false;
            }

            return gameTime - anchoredAtTick >= stuckTicks;
        }

        void reset()
        {
            anchor = null;
        }
    }
}
