package com.tik.zbb.ai.action.actions.gototarget;

import com.tik.zbb.ai.action.IMobAction;
import com.tik.zbb.ai.action.MobActionContext;
import com.tik.zbb.utilities.DistanceMultiplierUtility;
import com.tik.zbb.utilities.SecondsToTicksUtility;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

public class GoToTargetAction implements IMobAction<GoToTargetRequest>
{
    private static final float MAX_TARGET_MOVE_DISTANCE_TO_NEW_PATH = 2.0f;

    private Vec3 lastPathTargetPos;

    @Override
    public boolean canExecute(MobActionContext context, GoToTargetRequest request)
    {
        long now = context.level().getGameTime();

        boolean notFreezed = context.aiTimers().freezePassed(now);
        boolean cooldownPassed = context.aiTimers().goToTargetCooldownPassed(now);
        boolean isNewPathSimilar = isCurrentPathAlreadyGoodEnough(context.mob(), request.target());

        return notFreezed && cooldownPassed && !isNewPathSimilar;
    }

    @Override
    public void execute(MobActionContext context, GoToTargetRequest request)
    {
        context.mob().getNavigation().moveTo(request.target(), 1.0);
        lastPathTargetPos = request.target().position();

        double cooldownSeconds = DistanceMultiplierUtility.applyDistanceMultiplier(
                context.configSnapshot().data().balance.optimization.goToTargetInterval,
                context.mob().distanceTo(request.target()),
                context.configSnapshot().data().balance.optimization.distanceCooldownStartBlocks,
                context.configSnapshot().data().balance.optimization.distanceCooldownMaxBlocks,
                context.configSnapshot().data().balance.optimization.distanceCooldownMaxMultiplier);
        context.aiTimers().setGoToTargetCooldownUntil(context.level().getGameTime() + SecondsToTicksUtility.toTicks(cooldownSeconds, 1));
    }

    private boolean isCurrentPathAlreadyGoodEnough(PathfinderMob mob, LivingEntity target)
    {
        if (lastPathTargetPos == null) return false;

        PathNavigation nav = mob.getNavigation();
        Path path = nav.getPath();

        if (path == null || path.isDone()) return false;

        double distSq = target.distanceToSqr(lastPathTargetPos);
        return distSq < (MAX_TARGET_MOVE_DISTANCE_TO_NEW_PATH * MAX_TARGET_MOVE_DISTANCE_TO_NEW_PATH);
    }
}
