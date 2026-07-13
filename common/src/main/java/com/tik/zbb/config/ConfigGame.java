package com.tik.zbb.config;

import com.tik.zbb.config.schema.ResourceLocationPatternMatcher;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

import java.util.HashMap;
import java.util.Map;

public record ConfigGame(
        Blocks blocks,
        Ai ai,
        Balance balance,
        BlockRestoration blockRestoration,
        VisualEffects visualEffects
)
{
    public static ConfigGame create(ConfigDocument data)
    {
        return create(data, BlockResolver.NONE);
    }

    public static ConfigGame create(ConfigDocument data, BlockResolver blockResolver)
    {
        return new ConfigGame(
                Blocks.create(data, blockResolver),
                Ai.create(data),
                Balance.create(data, blockResolver),
                BlockRestoration.create(data),
                VisualEffects.create(data)
        );
    }

    public record Blocks(
            ResourceLocationPatternMatcher dangerousBlockIdMatcher,
            Map<Identifier, Block> dimensionPlaceBlockMap,
            Map<Identifier, Block> mobPlaceBlockOverrideMap,
            Block fallbackPlaceBlock
    )
    {
        private static Blocks create(ConfigDocument data, BlockResolver blockResolver)
        {
            return new Blocks(
                    ResourceLocationPatternMatcher.compile(data.blocks.dangerousBlockIdList),
                    idBlockMap(data.blocks.dimensionPlaceBlockIdList, blockResolver),
                    idBlockMap(data.blocks.mobPlaceBlockIdOverrideList, blockResolver),
                    blockResolver.resolve(data.blocks.fallbackPlaceBlockId, net.minecraft.world.level.block.Blocks.STONE)
            );
        }
    }

    public record Ai(
            boolean alwaysSeeNearestPlayer,
            boolean canNoticeTargetsThroughBlocks,
            int noticeTargetsThroughBlocksLimit,
            boolean canContinueSeeingTargetsThroughBlocks,
            int continueSeeingTargetsThroughBlocksLimit,
            ResourceLocationPatternMatcher affectedEntityIdMatcher,
            ResourceLocationPatternMatcher ignoreBuildEntityIdMatcher,
            ResourceLocationPatternMatcher ignoreBreakEntityIdMatcher
    )
    {
        private static Ai create(ConfigDocument data)
        {
            return new Ai(
                    data.ai.alwaysSeeNearestPlayer,
                    data.ai.canNoticeTargetsThroughBlocks,
                    data.ai.noticeTargetsThroughBlocksLimit,
                    data.ai.canContinueSeeingTargetsThroughBlocks,
                    data.ai.continueSeeingTargetsThroughBlocksLimit,
                    ResourceLocationPatternMatcher.compile(data.ai.affectedEntityIdList),
                    ResourceLocationPatternMatcher.compile(data.ai.ignoreBuildEntityIdList),
                    ResourceLocationPatternMatcher.compile(data.ai.ignoreBreakEntityIdList)
            );
        }
    }

    public record Balance(
            double builtBlocksProtectionTime,
            int dangerousBlocksSearchRadius,
            int pathEndBreakBuildDistance,
            double damageStoreTime,
            BlockDamage blockDamage,
            Cooldowns cooldowns
    )
    {
        private static Balance create(ConfigDocument data, BlockResolver blockResolver)
        {
            return new Balance(
                    data.balance.builtBlocksProtectionTime,
                    data.balance.dangerousBlocksSearchRadius,
                    data.balance.pathEndBreakBuildDistance,
                    data.balance.damageStoreTime,
                    BlockDamage.create(data, blockResolver),
                    Cooldowns.create(data)
            );
        }
    }

    public record BlockDamage(
            int damageToBlocks,
            float blockHardnessContrast,
            float blockHardnessMultiplier,
            Map<Block, Integer> blockHealthOverrideMap,
            float itemDamageMultiplierStrength,
            double hitboxSizeMultiplierStrength
    )
    {
        private static BlockDamage create(ConfigDocument data, BlockResolver blockResolver)
        {
            return new BlockDamage(
                    data.balance.blockDamage.damageToBlocks,
                    data.balance.blockDamage.blockHardnessContrast,
                    data.balance.blockDamage.blockHardnessMultiplier,
                    blockIntMap(data.balance.blockDamage.blockHealthOverrideList, blockResolver),
                    data.balance.blockDamage.itemDamageMultiplierStrength,
                    data.balance.blockDamage.hitboxSizeMultiplierStrength
            );
        }
    }

    public record Cooldowns(
            double breakCooldown,
            double buildCooldown,
            double searchDangerousBlocksCooldown
    )
    {
        private static Cooldowns create(ConfigDocument data)
        {
            return new Cooldowns(
                    data.balance.cooldowns.breakCooldown,
                    data.balance.cooldowns.buildCooldown,
                    data.balance.cooldowns.searchDangerousBlocksCooldown
            );
        }
    }

    public record BlockRestoration(
            boolean builtBlocksDisappearing,
            double builtBlocksDisappearTime,
            boolean brokenBlocksRestoring,
            double brokenBlocksRestoreTime
    )
    {
        private static BlockRestoration create(ConfigDocument data)
        {
            return new BlockRestoration(
                    data.blockRestoration.builtBlocksDisappearing,
                    data.blockRestoration.builtBlocksDisappearTime,
                    data.blockRestoration.brokenBlocksRestoring,
                    data.blockRestoration.brokenBlocksRestoreTime
            );
        }
    }

    public record VisualEffects(
            boolean breakMobSwing,
            boolean buildBlockSound,
            boolean brokenReappearParticles,
            boolean brokenReappearChargeSound,
            boolean brokenReappearSound,
            boolean builtDisappearBlockDisplay,
            boolean builtDisappearShrinkSound,
            boolean builtDisappearSound
    )
    {
        private static VisualEffects create(ConfigDocument data)
        {
            return new VisualEffects(
                    data.visualEffects.breakMobSwing,
                    data.visualEffects.buildBlockSound,
                    data.visualEffects.brokenReappearParticles,
                    data.visualEffects.brokenReappearChargeSound,
                    data.visualEffects.brokenReappearSound,
                    data.visualEffects.builtDisappearBlockDisplay,
                    data.visualEffects.builtDisappearShrinkSound,
                    data.visualEffects.builtDisappearSound
            );
        }
    }

    private static Map<Identifier, Block> idBlockMap(Map<String, String> entries, BlockResolver blockResolver)
    {
        Map<Identifier, Block> map = new HashMap<>();

        for (Map.Entry<String, String> entry : entries.entrySet())
        {
            Identifier key = Identifier.tryParse(entry.getKey());
            Block value = blockResolver.resolve(entry.getValue(), null);
            if (key != null && value != null) map.put(key, value);
        }

        return Map.copyOf(map);
    }

    private static Map<Block, Integer> blockIntMap(Map<String, Integer> entries, BlockResolver blockResolver)
    {
        Map<Block, Integer> map = new HashMap<>();

        for (Map.Entry<String, Integer> entry : entries.entrySet())
        {
            Block key = blockResolver.resolve(entry.getKey(), null);
            Integer value = entry.getValue();
            if (key != null && value != null && value >= 0) map.put(key, value);
        }

        return Map.copyOf(map);
    }

    public interface BlockResolver
    {
        BlockResolver NONE = (rawValue, defaultBlock) -> defaultBlock;
        BlockResolver MINECRAFT = (rawValue, defaultBlock) ->
        {
            Identifier id = Identifier.tryParse(rawValue);
            if (id == null) return defaultBlock;
            return BuiltInRegistries.BLOCK.get(id).map(Holder.Reference::value).orElse(defaultBlock);
        };

        Block resolve(String rawValue, Block defaultBlock);
    }
}
