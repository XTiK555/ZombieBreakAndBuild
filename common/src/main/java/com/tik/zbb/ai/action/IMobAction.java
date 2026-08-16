package com.tik.zbb.ai.action;

public interface IMobAction<R>
{
    boolean canExecute(MobActionContext context, R request);

    boolean execute(MobActionContext context, R request);
}
