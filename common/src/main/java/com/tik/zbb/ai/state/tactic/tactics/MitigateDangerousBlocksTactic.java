package com.tik.zbb.ai.state.tactic.tactics;

import com.tik.zbb.ai.state.MobStateContext;
import com.tik.zbb.ai.state.tactic.IMobTactic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;

public class MitigateDangerousBlocksTactic implements IMobTactic
{
    private Registry<Block> blockRegistry;
    private BlockPos.MutableBlockPos mobPos;
    private BlockPos.MutableBlockPos tmpBlockPos = new BlockPos.MutableBlockPos();
    private BlockPos.MutableBlockPos blockCover = new BlockPos.MutableBlockPos();

    @Override
    public void execute(MobStateContext context)
    {
        int radius = context.getConfigSnapshot().data().dangerousBlocksSearchRadius;
        mobPos = context.getMob().getOnPos().mutable();

        int baseX = mobPos.getX();
        int baseY = mobPos.getY();
        int baseZ = mobPos.getZ();

        for (int dy = -1; dy <= 1; dy++)
        {
            for (int dx = -radius; dx <= radius; dx++)
            {
                for (int dz = -radius; dz <= radius; dz++)
                {
                    tmpBlockPos.set(baseX + dx, baseY + dy, baseZ + dz);
                    BlockState blockState = context.getLevel().getBlockState(tmpBlockPos);

                    if (!isDangerous(blockState, context)) continue;

                    blockCover.set(tmpBlockPos.getX(), tmpBlockPos.getY() + 1, tmpBlockPos.getZ());

                    if (!context.getActionExecutor().tryExecuteBuildAction(tmpBlockPos))
                    {
                        context.getActionExecutor().tryExecuteBuildAction(blockCover);
                    }
                }
            }
        }
    }

    private boolean isDangerous(BlockState state, MobStateContext context)
    {
        if (!state.getFluidState().isEmpty()) return true;
        if (blockRegistry == null) blockRegistry = context.getLevel().registryAccess().lookupOrThrow(Registries.BLOCK);

        Identifier id = blockRegistry.getKey(state.getBlock());
        if (id == null) return false;

        if (!context.getConfigSnapshot().data().dangerousBlockIdSet.contains(id)) return false;

        if (state.getBlock() instanceof CampfireBlock)
        {
            return state.getValue(CampfireBlock.LIT);
        }

        return true;
    }
}
