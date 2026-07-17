package com.tik.zbb.ai.goals;

import com.tik.zbb.ai.AiTimers;
import com.tik.zbb.ai.action.ActionExecutor;
import com.tik.zbb.ai.state.MobStateHandler;
import com.tik.zbb.config.ConfigManager;
import com.tik.zbb.utilities.ShouldApplyToMobUtility;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

public class BreakAndBuildGoal extends Goal
{
    private final PathfinderMob mob;

    private AiTimers aiTimers;
    private ActionExecutor actionExecutor;
    private MobStateHandler stateHandler;

    public BreakAndBuildGoal(PathfinderMob mob)
    {
        this.mob = mob;
    }

    @Override
    public void tick()
    {
        LivingEntity target = mob.getTarget();
        if (target == null) return;

        ensureInitialized();
        actionExecutor.tick();
        stateHandler.tick(target);
    }

    @Override
    public void stop()
    {
        if (stateHandler != null)
        {
            stateHandler.resetTransientState();
        }
    }

    @Override
    public boolean canUse()
    {
        LivingEntity target = mob.getTarget();
        return target != null && target.isAlive() && ShouldApplyToMobUtility.matchesFullZbbMobFilter(mob, ConfigManager.getConfigSnapshot());
    }

    @Override
    public boolean canContinueToUse()
    {
        return canUse();
    }

    private void ensureInitialized()
    {
        if (stateHandler != null || actionExecutor != null || aiTimers != null) return;

        aiTimers = new AiTimers();
        actionExecutor = new ActionExecutor(mob, aiTimers);
        stateHandler = new MobStateHandler(actionExecutor, mob, aiTimers);
    }
}
