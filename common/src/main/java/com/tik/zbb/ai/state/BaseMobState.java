package com.tik.zbb.ai.state;

import com.tik.zbb.ai.state.tactic.IMobTactic;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseMobState
{
    protected List<IMobTactic> mobTactics = new ArrayList<IMobTactic>();

    public abstract void tick(MobStateContext context);

    public abstract Priority calculatePriority(MobStateContext context);

    public void resetTransientState()
    {
        for (int i = 0, size = mobTactics.size(); i < size; i++)
        {
            mobTactics.get(i).resetTransientState();
        }
    }
}
