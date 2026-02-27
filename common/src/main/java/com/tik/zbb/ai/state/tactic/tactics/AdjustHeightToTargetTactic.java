package com.tik.zbb.ai.state.tactic.tactics;

import com.tik.zbb.ai.state.MobStateContext;
import com.tik.zbb.ai.state.tactic.IMobTactic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;

import static com.tik.zbb.utilities.IsFreePassUtility.isFreePass;

public class AdjustHeightToTargetTactic implements IMobTactic
{
    private Registry<Block> blockRegistry;

    private BlockPos.MutableBlockPos tmpBlockPos = new BlockPos.MutableBlockPos();

    @Override
    public void execute(MobStateContext context)
    {
        if (blockRegistry == null) blockRegistry = context.getLevel().registryAccess().lookupOrThrow(Registries.BLOCK);

        int mobX = Mth.floor(context.getMob().getX());
        int mobY = Mth.floor(context.getMob().getY());
        int mobZ = Mth.floor(context.getMob().getZ());
        int targetY = Mth.floor(context.getTarget().getY());

        if (targetY > mobY + 1)
        {
            if (!isFreePass(tmpBlockPos.set(mobX, mobY + 1, mobZ), blockRegistry, context.getLevel(), context.getConfigSnapshot().data()))
                context.getActionExecutor().tryExecuteBreakAction(tmpBlockPos);
            if (!isFreePass(tmpBlockPos.set(mobX, mobY + 2, mobZ), blockRegistry, context.getLevel(), context.getConfigSnapshot().data()))
                context.getActionExecutor().tryExecuteBreakAction(tmpBlockPos);

            // If there is space above, we try to adjust and jump.
            if (isFreePass(tmpBlockPos.set(mobX, mobY + 2, mobZ), blockRegistry, context.getLevel(), context.getConfigSnapshot().data()))
            {
                if (context.getActionExecutor().tryExecuteBuildAction(tmpBlockPos.set(mobX, mobY, mobZ)))
                {
                    context.getMob().getJumpControl().jump();
                }
            }
            return;
        }

        // target below -> break the block below you (if it's preventing you from getting down)
        if (targetY < mobY - 1)
        {
            if (!isFreePass(tmpBlockPos.set(mobX, mobY - 1, mobZ), blockRegistry, context.getLevel(), context.getConfigSnapshot().data()))
            {
                context.getActionExecutor().tryExecuteBreakAction(tmpBlockPos);
            }
        }
    }
}
