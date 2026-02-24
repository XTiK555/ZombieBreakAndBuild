package com.tik.zbb.ai.action;

public interface IMobAction
{
    boolean canExecute(MobActionContext context);

    void execute(MobActionContext context);
}
