package com.tik.zbb.ai.action.actions.freeze;

import com.tik.zbb.ai.action.IMobAction;
import com.tik.zbb.ai.action.MobActionContext;
import com.tik.zbb.utilities.SecondsToTicksUtility;

public class FreezeAction implements IMobAction<FreezeRequest>
{
    @Override
    public boolean canExecute(MobActionContext context, FreezeRequest request)
    {
        return true;
    }

    @Override
    public boolean execute(MobActionContext context, FreezeRequest request)
    {
        int blockX = context.mob().blockPosition().getX();
        int blockZ = context.mob().blockPosition().getZ();

        double centerX = blockX + 0.5;
        double centerZ = blockZ + 0.5;
        double currentY = context.mob().getY();
        double currentVelY = context.mob().getDeltaMovement().y;

        context.mob().getMoveControl().setWantedPosition(centerX, currentY, centerZ, 0);
        context.mob().setDeltaMovement(0.0, currentVelY, 0.0);
        return true;
    }
}
