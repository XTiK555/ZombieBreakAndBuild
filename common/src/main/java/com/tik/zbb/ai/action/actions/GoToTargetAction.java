package com.tik.zbb.ai.action.actions;

import com.tik.zbb.ai.action.IMobAction;
import com.tik.zbb.ai.action.MobActionContext;
import com.tik.zbb.utilities.DistanceIntervalUtility;
import com.tik.zbb.utilities.SecondsToTicksUtility;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;

public class GoToTargetAction implements IMobAction
{
    private PathfinderMob mob;
    private LivingEntity target;

    @Override
    public boolean canExecute(MobActionContext context)
    {
        long now = context.level().getGameTime();

        boolean notFreezed = context.aiTimers().freezePassed(now);
        boolean cooldownPassed = context.aiTimers().goToTargetCooldownPassed(now);
        boolean isNewPathSimilar = isNewPathSimilar(context);

        return notFreezed && cooldownPassed && !isNewPathSimilar;
    }

    @Override
    public void execute(MobActionContext context)
    {
        mob.getNavigation().moveTo(target, 1.0);

        double cooldownSeconds = DistanceIntervalUtility.applyDistanceMultiplier(context.configSnapshot().data().balance.goToTargetInterval, mob.distanceTo(target), context.configSnapshot().data());
        context.aiTimers().setGoToTargetCooldownUntil(context.level().getGameTime() + SecondsToTicksUtility.toTicks(cooldownSeconds, 1));
    }

    public void setup(PathfinderMob mob, LivingEntity target)
    {
        this.mob = mob;
        this.target = target;
    }

    private boolean isNewPathSimilar(MobActionContext context)
    {
        PathNavigation nav = mob.getNavigation();
        Path path = nav.getPath();

        if (path != null && !path.isDone())
        {
            Node end = path.getEndNode();
            Node next = path.getNextNode();

            if (end != null && end != next)
            {
                double dx = end.x - target.getX();
                double dy = end.y - target.getY();
                double dz = end.z - target.getZ();

                if ((dx * dx + dy * dy + dz * dz) < 2.0 * 2.0)
                {
                    return true;
                }
            }
        }

        return false;
    }
}
