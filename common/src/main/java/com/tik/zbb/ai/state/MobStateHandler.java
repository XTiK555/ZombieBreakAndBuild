package com.tik.zbb.ai.state;

import com.tik.zbb.ai.AiTimers;
import com.tik.zbb.ai.action.ActionExecutor;
import com.tik.zbb.ai.state.states.BreakAndBuildState;
import com.tik.zbb.ai.state.states.DefaultState;
import com.tik.zbb.config.ConfigManager;
import com.tik.zbb.config.ConfigSnapshot;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;

import java.util.ArrayList;
import java.util.List;

public class MobStateHandler
{
    private final List<BaseMobState> mobStateList = new ArrayList<>();
    private final MobStateContext mobStateContext;

    private LivingEntity lastTarget;

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
        if (lastTarget != null && lastTarget != target) resetTransientState();

        keepDataUpToDate();
        mobStateContext.setTarget(target);

        BaseMobState bestState = null;
        Priority bestPriority = null;

        for (int i = 0, size = mobStateList.size(); i < size; i++)
        {
            BaseMobState state = mobStateList.get(i);
            Priority priority = state.calculatePriority(mobStateContext);
            if (priority == null) continue;

            if (bestPriority == null || priority.weight() > bestPriority.weight())
            {
                bestPriority = priority;
                bestState = state;
            }
        }

        if (bestState != null) bestState.tick(mobStateContext);

        lastTarget = target;
    }

    public void resetTransientState()
    {
        for (int i = 0, size = mobStateList.size(); i < size; i++)
        {
            mobStateList.get(i).resetTransientState();
        }
    }

    private void keepDataUpToDate()
    {
        if (!(mobStateContext.getMob().level() instanceof ServerLevel level))
            throw new IllegalStateException("The mob level is not the serverLevel.");

        ConfigSnapshot configSnapshot = ConfigManager.getConfigSnapshot();

        if (mobStateContext.getLevel() != level) mobStateContext.setLevel(level);
        if (mobStateContext.getConfigSnapshot().version() != configSnapshot.version())
            mobStateContext.setConfigSnapshot(configSnapshot);
    }
}
