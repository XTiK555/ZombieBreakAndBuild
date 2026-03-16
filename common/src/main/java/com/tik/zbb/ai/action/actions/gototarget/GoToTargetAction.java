package com.tik.zbb.ai.action.actions.gototarget;

import com.tik.zbb.ai.action.IMobAction;
import com.tik.zbb.ai.action.MobActionContext;
import com.tik.zbb.utilities.DistanceMultiplierUtility;
import com.tik.zbb.utilities.SecondsToTicksUtility;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

public class GoToTargetAction implements IMobAction<GoToTargetRequest>
{
    private static final double BASE_TARGET_REPATH_DISTANCE = 2.0;

    private Vec3 lastPathTargetPos;

    @Override
    public boolean canExecute(MobActionContext context, GoToTargetRequest request)
    {
        long now = context.level().getGameTime();

        boolean notFreezed = context.aiTimers().freezePassed(now);
        boolean cooldownPassed = context.aiTimers().goToTargetCooldownPassed(now);
        boolean isCurrentPathAlreadyGoodEnough = isCurrentPathAlreadyGoodEnough(context, request.target());

        return notFreezed && cooldownPassed && !isCurrentPathAlreadyGoodEnough;
    }

    @Override
    public void execute(MobActionContext context, GoToTargetRequest request)
    {
        context.mob().getNavigation().moveTo(request.target(), 1.0);
        lastPathTargetPos = request.target().position();

        double cooldownSeconds = DistanceMultiplierUtility.applyDistanceMultiplier(
                context.configSnapshot().data().balance.optimization.goToTargetInterval,
                context.mob().distanceTo(request.target()),
                context.configSnapshot().data().balance.optimization.distanceScaleStartBlocks,
                context.configSnapshot().data().balance.optimization.distanceScaleMaxBlocks,
                context.configSnapshot().data().balance.optimization.distanceScaleMaxMultiplier);
        context.aiTimers().setGoToTargetCooldownUntil(context.level().getGameTime() + SecondsToTicksUtility.toTicks(cooldownSeconds, 1));
    }

    private boolean isCurrentPathAlreadyGoodEnough(MobActionContext context, LivingEntity target)
    {
        if (lastPathTargetPos == null) return false;

        PathNavigation nav = context.mob().getNavigation();
        Path path = nav.getPath();

        if (path == null || path.isDone()) return false;

        double targetRepathDistance = getTargetRepathDistance(context, target);
        double distSq = target.distanceToSqr(lastPathTargetPos);
        return distSq <= (targetRepathDistance * targetRepathDistance);
    }

    private double getTargetRepathDistance(MobActionContext context, LivingEntity target)
    {
        double distanceToTarget = context.mob().distanceTo(target);

        return DistanceMultiplierUtility.applyDistanceMultiplier(
                BASE_TARGET_REPATH_DISTANCE,
                distanceToTarget,
                context.configSnapshot().data().balance.optimization.distanceScaleStartBlocks,
                context.configSnapshot().data().balance.optimization.distanceScaleMaxBlocks,
                context.configSnapshot().data().balance.optimization.distanceScaleMaxMultiplier
        );
    }
}
