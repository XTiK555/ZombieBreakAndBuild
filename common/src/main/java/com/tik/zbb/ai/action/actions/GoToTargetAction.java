package com.tik.zbb.ai.action.actions;

import com.tik.zbb.ai.action.IMobAction;
import com.tik.zbb.ai.action.MobActionContext;
import com.tik.zbb.utilities.DistanceMultiplierUtility;
import com.tik.zbb.utilities.SecondsToTicksUtility;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

public class GoToTargetAction implements IMobAction
{
    private static final float MAX_TARGET_MOVE_DISTANCE_TO_NEW_PATH = 2.0f;

    private PathfinderMob mob;
    private LivingEntity target;
    private Vec3 lastPathTargetPos;

    @Override
    public boolean canExecute(MobActionContext context)
    {
        long now = context.level().getGameTime();

        boolean notFreezed = context.aiTimers().freezePassed(now);
        boolean cooldownPassed = context.aiTimers().goToTargetCooldownPassed(now);
        boolean isNewPathSimilar = isCurrentPathAlreadyGoodEnough();

        return notFreezed && cooldownPassed && !isNewPathSimilar;
    }

    @Override
    public void execute(MobActionContext context)
    {
        mob.getNavigation().moveTo(target, 1.0);
        lastPathTargetPos = target.position();

        double cooldownSeconds = DistanceMultiplierUtility.applyDistanceMultiplier(
                context.configSnapshot().data().balance.goToTargetInterval,
                mob.distanceTo(target),
                context.configSnapshot().data().balance.distanceCooldownStartBlocks,
                context.configSnapshot().data().balance.distanceCooldownMaxBlocks,
                context.configSnapshot().data().balance.distanceCooldownMaxMultiplier);
        context.aiTimers().setGoToTargetCooldownUntil(context.level().getGameTime() + SecondsToTicksUtility.toTicks(cooldownSeconds, 1));
    }

    public void setup(PathfinderMob mob, LivingEntity target)
    {
        this.mob = mob;
        this.target = target;
    }

    private boolean isCurrentPathAlreadyGoodEnough()
    {
        if (lastPathTargetPos == null) return false;

        PathNavigation nav = mob.getNavigation();
        Path path = nav.getPath();

        if (path == null || path.isDone()) return false;

        double distSq = target.distanceToSqr(lastPathTargetPos);
        return distSq < (MAX_TARGET_MOVE_DISTANCE_TO_NEW_PATH * MAX_TARGET_MOVE_DISTANCE_TO_NEW_PATH);
    }
}
