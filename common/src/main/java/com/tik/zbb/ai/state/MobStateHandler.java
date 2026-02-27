package com.tik.zbb.ai.state;

import com.tik.zbb.ai.AiTimers;
import com.tik.zbb.ai.action.ActionExecutor;
import com.tik.zbb.ai.action.MobActionContext;
import com.tik.zbb.ai.state.states.BreakAndBuildState;
import com.tik.zbb.ai.state.states.DefaultState;
import com.tik.zbb.config.ConfigManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;

import java.util.ArrayList;
import java.util.List;

public class MobStateHandler
{
    private final List<IMobState> mobStateList = new ArrayList<>();
    private MobStateContext mobStateContext;

    public MobStateHandler(ActionExecutor actionExecutor, PathfinderMob mob, AiTimers aiTimers)
    {
        if (!(mob.level() instanceof ServerLevel level))
            throw new IllegalStateException("The mob level is not the serverLevel.");

        mobStateContext = new MobStateContext(
                actionExecutor,
                mob,
                level,
                aiTimers,
                ConfigManager.getConfigSnapshot()
        );

        mobStateList.add(new DefaultState());
        mobStateList.add(new BreakAndBuildState());
    }

    public void tick(LivingEntity target)
    {
        keepDataUpToDate();
        mobStateContext.setTarget(target);

        IMobState bestState = null;
        Priority bestPriority = null;

        for (IMobState state : mobStateList)
        {
            Priority p = state.calculatePriority(mobStateContext);
            if (p == null) continue;

            if (bestPriority == null || p.weight() > bestPriority.weight())
            {
                bestPriority = p;
                bestState = state;
            }
        }

        if (bestState != null) bestState.tick(mobStateContext);
    }

    private void keepDataUpToDate()
    {
        if (!(mobStateContext.getMob().level() instanceof ServerLevel level))
            throw new IllegalStateException("The mob level is not the serverLevel.");

        if (mobStateContext.getLevel() != level) mobStateContext.setLevel(level);
        if (mobStateContext.getConfigSnapshot().version() != ConfigManager.getConfigSnapshot().version())
            mobStateContext.setConfigSnapshot(ConfigManager.getConfigSnapshot());
    }
}
