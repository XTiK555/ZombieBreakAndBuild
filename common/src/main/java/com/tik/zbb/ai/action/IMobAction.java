package com.tik.zbb.ai.action;

public interface IMobAction<R>
{
    boolean canExecute(MobActionContext context, R request);

    void execute(MobActionContext context, R request);
}
