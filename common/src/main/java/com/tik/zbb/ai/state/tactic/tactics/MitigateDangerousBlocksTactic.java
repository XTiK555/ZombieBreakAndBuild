package com.tik.zbb.ai.state.tactic.tactics;

import com.tik.zbb.ai.state.MobStateContext;
import com.tik.zbb.ai.state.tactic.IMobTactic;
import com.tik.zbb.utilities.SecondsToTicksUtility;
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

    private final BlockPos.MutableBlockPos scanPos = new BlockPos.MutableBlockPos();
    private final BlockPos.MutableBlockPos coverPos = new BlockPos.MutableBlockPos();

    @Override
    public void execute(MobStateContext context)
    {
        long now = context.getLevel().getGameTime();
        if (!context.getAiTimers().mitigateDangerousBlocksCooldownPassed(now)) return;

        if (blockRegistry == null) blockRegistry = context.getLevel().registryAccess().lookupOrThrow(Registries.BLOCK);
        int radius = context.getConfigSnapshot().data().ai.dangerousBlocksSearchRadius;
        int mobX = context.getMob().getBlockX();
        int mobY = context.getMob().getBlockY();
        int mobZ = context.getMob().getBlockZ();

        for (int dy = -radius; dy <= radius; dy++)
        {
            for (int dx = -radius; dx <= radius; dx++)
            {
                for (int dz = -radius; dz <= radius; dz++)
                {
                    scanPos.set(mobX + dx, mobY + dy, mobZ + dz);
                    BlockState blockState = context.getLevel().getBlockState(scanPos);

                    if (!isDangerous(blockState, context)) continue;

                    handleDangerousBlock(scanPos, context);
                }
            }
        }

        context.getAiTimers().setMitigateDangerousBlocksCooldownUntil(now + SecondsToTicksUtility.toTicks(context.getConfigSnapshot().data().balance.searchDangerousBlocksInterval, 1));
    }

    private void handleDangerousBlock(BlockPos blockPos, MobStateContext context)
    {
        if (!context.getActionExecutor().tryExecuteBuildAction(blockPos))
        {
            if (!context.getActionExecutor().tryExecuteBreakAction(blockPos))
            {
                coverPos.set(blockPos.getX(), blockPos.getY() + 1, blockPos.getZ());
                context.getActionExecutor().tryExecuteBuildAction(coverPos);
            }
        }
    }

    private boolean isDangerous(BlockState state, MobStateContext context)
    {
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
