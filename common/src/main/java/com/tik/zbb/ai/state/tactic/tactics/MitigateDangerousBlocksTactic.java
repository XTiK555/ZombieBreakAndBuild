package com.tik.zbb.ai.state.tactic.tactics;

import com.tik.zbb.ai.state.MobStateContext;
import com.tik.zbb.ai.state.tactic.IMobTactic;
import com.tik.zbb.utilities.HitboxScanUtility;
import com.tik.zbb.utilities.SecondsToTicksUtility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class MitigateDangerousBlocksTactic implements IMobTactic
{
    @Override
    public void execute(MobStateContext context)
    {
        long now = context.getLevel().getGameTime();
        if (!context.getAiTimers().mitigateDangerousBlocksCooldownPassed(now)) return;

        int radius = context.getConfigSnapshot().game().balance().dangerousBlocksSearchRadius();

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

        context.getAiTimers().setMitigateDangerousBlocksCooldownUntil(now + SecondsToTicksUtility.toTicks(context.getConfigSnapshot().game().balance().cooldowns().searchDangerousBlocksCooldown(), 1));
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
        Registry<Block> blockRegistry = context.getLevel().registryAccess().lookupOrThrow(Registries.BLOCK);
        Identifier id = blockRegistry.getKey(state.getBlock());

        if (id == null) return false;
        if (!context.getConfigSnapshot().game().blocks().dangerousBlockIdMatcher().matches(id)) return false;

        return true;
    }
}
