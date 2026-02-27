package com.tik.zbb.ai.state.tactic.tactics;

import com.tik.zbb.ai.state.MobStateContext;
import com.tik.zbb.ai.state.tactic.IMobTactic;

public class GoToTargetTactic implements IMobTactic
{
    @Override
    public void execute(MobStateContext context)
    {
        context.getActionExecutor().tryExecuteGoToTargetAction(context.getTarget());
    }
}
