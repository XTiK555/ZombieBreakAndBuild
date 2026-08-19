package com.tik.zbb.ai.state.tactic.tactics;

import com.tik.zbb.ai.state.MobStateContext;
import com.tik.zbb.ai.state.tactic.IMobTactic;
import com.tik.zbb.config.ConfigSnapshot;
import com.tik.zbb.utilities.HitboxScanUtility;
import com.tik.zbb.utilities.SecondsToTicksUtility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MitigateDangerousBlocksTactic implements IMobTactic
{
    private static final Object DANGEROUS_BLOCK_CACHE_LOCK = new Object();
    private static volatile DangerousBlockCache dangerousBlockCache = new DangerousBlockCache(Long.MIN_VALUE, new ConcurrentHashMap<>());

    private BlockPos lastDangerousBlockPos;

    @Override
    public void execute(MobStateContext context)
    {
        if (!context.getConfigSnapshot().game().ai().tactics().mitigateDangerousBlocks()) return;
        if (!context.getAiTimers().mitigateDangerousBlocksCooldownPassed(context.getLevel().getGameTime())) return;

        int radius = context.getConfigSnapshot().game().balance().dangerousBlocksSearchRadius();
        BlockPos dangerousBlockPos = findDangerousBlock(context, radius);
        lastDangerousBlockPos = dangerousBlockPos;

        if (dangerousBlockPos != null)
        {
            handleDangerousBlock(dangerousBlockPos, context);
        }

        context.getAiTimers().setMitigateDangerousBlocksCooldownUntil(context.getLevel().getGameTime() + SecondsToTicksUtility.toTicks(context.getConfigSnapshot().game().balance().cooldowns().searchDangerousBlocksCooldown(), 1));
    }

    private BlockPos findDangerousBlock(MobStateContext context, int radius)
    {
        if (lastDangerousBlockPos != null
                && context.getMob().getBoundingBox().inflate(radius).intersects(new AABB(lastDangerousBlockPos))
                && isDangerous(context.getLevel().getBlockState(lastDangerousBlockPos), context))
        {
            return lastDangerousBlockPos;
        }

        return HitboxScanUtility.findNearestBlockInHitbox(context.getLevel(), context.getMob(), radius, state -> isDangerous(state, context));
    }

    @Override
    public void resetTransientState()
    {
        lastDangerousBlockPos = null;
    }

    private void handleDangerousBlock(BlockPos blockPos, MobStateContext context)
    {
        if (!context.getActionExecutor().tryExecuteBuildAction(blockPos))
        {
            context.getActionExecutor().tryExecuteBreakAction(blockPos);
        }
    }

    private static boolean isDangerous(BlockState state, MobStateContext context)
    {
        ConfigSnapshot snapshot = context.getConfigSnapshot();
        long requestedVersion = snapshot.version();

        DangerousBlockCache cache = dangerousBlockCache;

        if (cache.configVersion() < requestedVersion)
        {
            synchronized (DANGEROUS_BLOCK_CACHE_LOCK)
            {
                cache = dangerousBlockCache;

                if (cache.configVersion() < requestedVersion)
                {
                    cache = new DangerousBlockCache(requestedVersion, new ConcurrentHashMap<>());
                    dangerousBlockCache = cache;
                }
            }
        }

        if (cache.configVersion() != requestedVersion)
        {
            return calculateDangerous(state.getBlock(), context, snapshot);
        }

        Boolean match = cache.matchesByBlock().get(state.getBlock());
        if (match != null)
        {
            return match;
        }

        boolean result = calculateDangerous(state.getBlock(), context, snapshot);
        cache.matchesByBlock().put(state.getBlock(), result);
        return result;
    }

    private static boolean calculateDangerous(Block block, MobStateContext context, ConfigSnapshot snapshot)
    {
        Registry<Block> registry = context.getLevel().registryAccess().lookupOrThrow(Registries.BLOCK);
        Identifier id = registry.getKey(block);

        return snapshot.game().blocks().dangerousBlockIdMatcher().matches(id);
    }

    private record DangerousBlockCache(long configVersion, ConcurrentMap<Block, Boolean> matchesByBlock) {}
}
