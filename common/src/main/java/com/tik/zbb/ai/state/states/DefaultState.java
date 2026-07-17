package com.tik.zbb.ai.state.states;

import com.tik.zbb.ai.state.BaseMobState;
import com.tik.zbb.ai.state.MobStateContext;
import com.tik.zbb.ai.state.Priority;
import com.tik.zbb.ai.state.tactic.IMobTactic;
import com.tik.zbb.ai.state.tactic.tactics.MitigateDangerousBlocksTactic;

public class DefaultState extends BaseMobState
{
    public DefaultState()
    {
        mobTactics.add(new MitigateDangerousBlocksTactic());

        resetTransientState();
    }

    @Override
    public void tick(MobStateContext context)
    {
        for (IMobTactic mobTactic : mobTactics)
        {
            mobTactic.execute(context);
        }
    }

    @Override
    public Priority calculatePriority(MobStateContext context)
    {
        return Priority.Medium;
    }
}
