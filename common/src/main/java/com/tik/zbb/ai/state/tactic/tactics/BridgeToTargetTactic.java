package com.tik.zbb.ai.state.tactic.tactics;

import com.tik.zbb.ai.state.MobStateContext;
import com.tik.zbb.ai.state.tactic.IMobTactic;
import com.tik.zbb.utilities.GetHorizontalFrontBlockUtility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.world.level.block.Block;

public class BridgeToTargetTactic implements IMobTactic
{
    private Registry<Block> blockRegistry;

    private BlockPos.MutableBlockPos tmpBlockPos = new BlockPos.MutableBlockPos();
    private BlockPos.MutableBlockPos frontBlockPos = new BlockPos.MutableBlockPos();

    @Override
    public void execute(MobStateContext context)
    {
        frontBlockPos = GetHorizontalFrontBlockUtility.getPos(context.getMob().getOnPos(), context.getTarget().getOnPos()).mutable();

        boolean belowUsAir = context.getLevel().getBlockState(tmpBlockPos.set(context.getMob().getX(), frontBlockPos.getY() - 1, context.getMob().getZ())).isAir();
        if (belowUsAir && context.getTarget().getY() >= frontBlockPos.getY())
        {
            context.getActionExecutor().tryExecuteBuildAction(tmpBlockPos.set(context.getMob().getX(), frontBlockPos.getY() - 1, context.getMob().getZ()));
        }

        boolean frontAir = context.getLevel().getBlockState(frontBlockPos).isAir();
        boolean belowAir = context.getLevel().getBlockState(tmpBlockPos.set(frontBlockPos.getX(), frontBlockPos.getY() - 1, frontBlockPos.getZ())).isAir();
        boolean below2Air = context.getLevel().getBlockState(tmpBlockPos.set(frontBlockPos.getX(), frontBlockPos.getY() - 2, frontBlockPos.getZ())).isAir();
        if (frontAir && belowAir && below2Air)
        {
            context.getActionExecutor().tryExecuteBuildAction(tmpBlockPos.set(frontBlockPos.getX(), frontBlockPos.getY() - 1, frontBlockPos.getZ()));
        }
    }
}
