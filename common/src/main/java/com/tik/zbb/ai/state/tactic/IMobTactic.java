package com.tik.zbb.ai.state.tactic;

import com.tik.zbb.ai.state.MobStateContext;

public interface IMobTactic
{
    void execute(MobStateContext context);

    default boolean isRunning()
    {
        return false;
    }

    default void resetTransientState()
    {
    }
}
