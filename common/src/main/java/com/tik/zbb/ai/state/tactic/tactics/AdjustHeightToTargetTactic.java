package com.tik.zbb.ai.state.tactic.tactics;

import com.tik.zbb.ai.state.MobStateContext;
import com.tik.zbb.ai.state.tactic.IMobTactic;
import com.tik.zbb.utilities.HitboxScanUtility;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import static com.tik.zbb.utilities.IsFreePassUtility.isFreePass;

public class AdjustHeightToTargetTactic implements IMobTactic
{
    private BlockPos.MutableBlockPos tmpBlockPos = new BlockPos.MutableBlockPos();

    @Override
    public void execute(MobStateContext context)
    {
        int mobX = Mth.floor(context.getMob().getX());
        int mobY = Mth.floor(context.getMob().getY());
        int mobZ = Mth.floor(context.getMob().getZ());
        int targetY = Mth.floor(context.getTarget().getY());

        if (targetY > mobY + 1)
        {
            BlockPos blockAboveUs = HitboxScanUtility.getNearestCollidingBlock(context.getLevel(), context.getMob(), new Vec3(0, 1, 0));

            // If there is space above
            if (blockAboveUs == null || isFreePass(blockAboveUs, context.getLevel()))
            {
                if (context.getActionExecutor().tryExecuteBuildAction(tmpBlockPos.set(mobX, mobY, mobZ)))
                {
                    context.getMob().getJumpControl().jump();
                }
                else
                {
                    context.getActionExecutor().tryExecuteBreakAction(tmpBlockPos.set(mobX, mobY, mobZ));
                }
            }
        }
    }
}
