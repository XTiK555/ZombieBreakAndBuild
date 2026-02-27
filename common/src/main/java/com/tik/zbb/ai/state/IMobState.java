package com.tik.zbb.ai.state;

public interface IMobState
{
    void tick(MobStateContext context);

    Priority calculatePriority(MobStateContext context);
}
