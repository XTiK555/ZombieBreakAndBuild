package com.tik.zbb.ai.state.states;

import com.tik.zbb.ai.state.IMobState;
import com.tik.zbb.ai.state.MobStateContext;
import com.tik.zbb.ai.state.Priority;
import com.tik.zbb.ai.state.tactic.tactics.*;

public class DefaultState implements IMobState
{
    private final GoToTargetTactic goToTargetTactic = new GoToTargetTactic();
    private final MitigateDangerousBlocksTactic mitigateDangerousBlocksTactic = new MitigateDangerousBlocksTactic();

    @Override
    public void tick(MobStateContext context)
    {
        mitigateDangerousBlocksTactic.execute(context);
        goToTargetTactic.execute(context);
    }

    @Override
    public Priority calculatePriority(MobStateContext context)
    {
        return Priority.Medium;
    }
}
