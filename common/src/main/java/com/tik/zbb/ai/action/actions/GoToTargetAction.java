package com.tik.zbb.ai.action.actions;

import com.tik.zbb.ai.action.IMobAction;
import com.tik.zbb.ai.action.MobActionContext;
import com.tik.zbb.utilities.SecondsToTicksUtility;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;

public class GoToTargetAction implements IMobAction
{
    private PathfinderMob mob;
    private LivingEntity target;

    @Override
    public boolean canExecute(MobActionContext context)
    {
        long now = context.level().getGameTime();

        boolean notFreezed = context.actionTimers().freezePassed(now);
        boolean cooldownPassed = context.actionTimers().goToTargetCooldownPassed(now);

        return notFreezed && cooldownPassed;
    }

    @Override
    public void execute(MobActionContext context)
    {
        mob.getNavigation().moveTo(target, 1.0);
        context.actionTimers().setGoToTargetCooldownUntil(context.level().getGameTime() + SecondsToTicksUtility.toTicks(context.configSnapshot().data().goToTargetInterval, 1));
    }

    public void setup(PathfinderMob mob, LivingEntity target)
    {
        this.mob = mob;
        this.target = target;
    }
}
