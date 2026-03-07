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
        int bx = context.mob().blockPosition().getX();
        int bz = context.mob().blockPosition().getZ();
        double cx = bx + 0.5;
        double cz = bz + 0.5;

        context.mob().getMoveControl().setWantedPosition(cx, context.mob().getY(), cz, 1);
        context.mob().getNavigation().stop();
        context.mob().setDeltaMovement(0.0, context.mob().getDeltaMovement().y, 0.0);

        context.aiTimers().setFreezeUntil(context.level().getGameTime() + SecondsToTicksUtility.toTicks(context.configSnapshot().data().balance.freezeTime));
    }
}
