package com.tik.zbb.ai.state.tactic.tactics;

import com.tik.zbb.ai.state.MobStateContext;
import com.tik.zbb.ai.state.tactic.IMobTactic;
import com.tik.zbb.utilities.HitboxScanUtility;
import com.tik.zbb.utilities.SecondsToTicksUtility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;

public class MitigateDangerousBlocksTactic implements IMobTactic
{
    @Override
    public void execute(MobStateContext context)
    {
        long now = context.getLevel().getGameTime();
        if (!context.getAiTimers().mitigateDangerousBlocksCooldownPassed(now)) return;

        int radius = context.getConfigSnapshot().data().balance.dangerousBlocksSearchRadius;

        BlockPos dangerousBlockPos = HitboxScanUtility.findNearestBlockInHitbox(
                context.getLevel(),
                context.getMob(),
                radius,
                state -> isDangerous(state, context)
        );

        if (dangerousBlockPos != null)
        {
            handleDangerousBlock(dangerousBlockPos, context);
        }

        context.getAiTimers().setMitigateDangerousBlocksCooldownUntil(now + SecondsToTicksUtility.toTicks(context.getConfigSnapshot().data().balance.cooldowns.searchDangerousBlocksCooldown, 1));
    }

    private void handleDangerousBlock(BlockPos blockPos, MobStateContext context)
    {
        if (!context.getActionExecutor().tryExecuteBuildAction(blockPos))
        {
            context.getActionExecutor().tryExecuteBreakAction(blockPos);
        }
    }

    private boolean isDangerous(BlockState state, MobStateContext context)
    {
        Registry<Block> blockRegistry = context.getLevel().registryAccess().registryOrThrow(Registries.BLOCK);
        ResourceLocation id = blockRegistry.getKey(state.getBlock());

        if (id == null) return false;
        if (!context.getConfigSnapshot().data().dangerousBlockIdSet.contains(id)) return false;
        if (state.getBlock() instanceof CampfireBlock)
        {
            return state.getValue(CampfireBlock.LIT);
        }

        return true;
    }
}
