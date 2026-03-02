package com.tik.zbb.ai.state.tactic.tactics;

import com.tik.zbb.ai.state.MobStateContext;
import com.tik.zbb.ai.state.tactic.IMobTactic;
import com.tik.zbb.utilities.GetHorizontalFrontBlockUtility;
import com.tik.zbb.utilities.IsFreePassUtility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;

public class ClearObstaclesToTargetTactic implements IMobTactic
{
    private Registry<Block> blockRegistry;

    private BlockPos.MutableBlockPos mobPos = new BlockPos.MutableBlockPos();
    private BlockPos.MutableBlockPos tmpBlockPos = new BlockPos.MutableBlockPos();
    private BlockPos.MutableBlockPos frontBlockPos = new BlockPos.MutableBlockPos();

    @Override
    public void execute(MobStateContext context)
    {
        if (blockRegistry == null) blockRegistry = context.getLevel().registryAccess().lookupOrThrow(Registries.BLOCK);
        mobPos = context.getMob().blockPosition().mutable();

        frontBlockPos = GetHorizontalFrontBlockUtility.getPos(mobPos, context.getTarget().blockPosition()).mutable();

        if (!IsFreePassUtility.isFreePass(mobPos, blockRegistry, context.getLevel(), context.getConfigSnapshot().data()))
        {
            context.getActionExecutor().tryExecuteBreakAction(mobPos);
        }
        tmpBlockPos.set(mobPos.getX(), mobPos.getY() + 1, mobPos.getZ());
        if (!IsFreePassUtility.isFreePass(tmpBlockPos, blockRegistry, context.getLevel(), context.getConfigSnapshot().data()))
        {
            context.getActionExecutor().tryExecuteBreakAction(tmpBlockPos);
        }

        // we break the block right in front of us if it is impassable
        if (!IsFreePassUtility.isFreePass(frontBlockPos, blockRegistry, context.getLevel(), context.getConfigSnapshot().data()))
        {
            context.getActionExecutor().tryExecuteBreakAction(frontBlockPos);
        }
        // we break the block above the front one if it also interferes
        tmpBlockPos.set(frontBlockPos.getX(), frontBlockPos.getY() + 1, frontBlockPos.getZ());
        if (!IsFreePassUtility.isFreePass(tmpBlockPos, blockRegistry, context.getLevel(), context.getConfigSnapshot().data()))
        {
            context.getActionExecutor().tryExecuteBreakAction(tmpBlockPos);
        }
    }
}
