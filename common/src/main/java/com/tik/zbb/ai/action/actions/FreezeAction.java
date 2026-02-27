package com.tik.zbb.ai.action.actions;

import com.tik.zbb.ai.action.IMobAction;
import com.tik.zbb.ai.action.MobActionContext;
import com.tik.zbb.utilities.SecondsToTicksUtility;

public class FreezeAction implements IMobAction
{
    @Override
    public boolean canExecute(MobActionContext context)
    {
        return true;
    }

    @Override
    public void execute(MobActionContext context)
    {
        context.aiTimers().setFreezeUntil(context.level().getGameTime() + SecondsToTicksUtility.toTicks(context.configSnapshot().data().freezeTime));

        context.mob().getNavigation().stop();
        context.mob().getMoveControl().setWantedPosition(context.mob().getX(), context.mob().getY(), context.mob().getZ(), 0.0);
        context.mob().setDeltaMovement(0.0, context.mob().getDeltaMovement().y, 0.0);
    }
}
