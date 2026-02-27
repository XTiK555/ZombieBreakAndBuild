package com.tik.zbb.ai.state.tactic.tactics;

import com.tik.zbb.ai.state.MobStateContext;
import com.tik.zbb.ai.state.tactic.IMobTactic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
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

        if (context.getTarget().getY() > context.getMob().getY() + 1)
        {
            if (!isFreePass(tmpBlockPos.set(context.getMob().getX(), context.getMob().getY() + 1, context.getMob().getZ()), blockRegistry, context.getLevel(), context.getConfigSnapshot().data()))
                context.getActionExecutor().tryExecuteBreakAction(tmpBlockPos);
            if (!isFreePass(tmpBlockPos.set(context.getMob().getX(), context.getMob().getY() + 2, context.getMob().getZ()), blockRegistry, context.getLevel(), context.getConfigSnapshot().data()))
                context.getActionExecutor().tryExecuteBreakAction(tmpBlockPos);

            // If there is space above, we try to adjust and jump.
            if (isFreePass(tmpBlockPos.set(context.getMob().getX(), context.getMob().getY() + 2, context.getMob().getZ()), blockRegistry, context.getLevel(), context.getConfigSnapshot().data()))
            {
                if (context.getActionExecutor().tryExecuteBuildAction(tmpBlockPos.set(context.getMob().getX(), context.getMob().getY(), context.getMob().getZ())))
                {
                    context.getMob().getJumpControl().jump();
                }
            }
            return;
        }

        // target below -> break the block below you (if it's preventing you from getting down)
        if (context.getTarget().getY() < context.getMob().getY() - 1)
        {
            if (!isFreePass(tmpBlockPos.set(context.getMob().getX(), context.getMob().getY() - 1, context.getMob().getZ()), blockRegistry, context.getLevel(), context.getConfigSnapshot().data()))
            {
                context.getActionExecutor().tryExecuteBreakAction(tmpBlockPos);
            }
        }
    }
}
