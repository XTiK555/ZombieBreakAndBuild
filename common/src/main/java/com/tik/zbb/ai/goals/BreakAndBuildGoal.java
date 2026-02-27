package com.tik.zbb.ai.goals;

import com.tik.zbb.ai.AiTimers;
import com.tik.zbb.ai.action.ActionExecutor;
import com.tik.zbb.ai.state.MobStateHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

public class BreakAndBuildGoal extends Goal
{
    private final PathfinderMob mob;
    private final AiTimers aiTimers = new AiTimers();
    private final ActionExecutor actionExecutor;
    private final MobStateHandler stateHandler;

    public BreakAndBuildGoal(PathfinderMob mob)
    {
        this.mob = mob;
        this.actionExecutor = new ActionExecutor(mob, aiTimers);
        this.stateHandler = new MobStateHandler(actionExecutor, mob, aiTimers);
    }

    @Override
    public void tick()
    {
        LivingEntity target = mob.getTarget();
        if (target == null) return;
        if (!(mob.level() instanceof ServerLevel)) return;

        stateHandler.tick(target);
    }

    @Override
    public boolean canUse()
    {
        LivingEntity target = mob.getTarget();
        return target != null && target.isAlive();
    }
}
