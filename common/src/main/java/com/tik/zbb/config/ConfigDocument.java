package com.tik.zbb.config;

import com.tik.zbb.config.annotations.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ConfigDocument
{
    public Blocks blocks = new Blocks();
    public Ai ai = new Ai();
    public Balance balance = new Balance();
    public BlockRestoration blockRestoration = new BlockRestoration();
    public VisualEffects visualEffects = new VisualEffects();

    public static class Blocks
    {
        @ResourceLocationPairMap
        @ResourceLocationSemantics(key = ResourceLocationRegistry.DIMENSION, value = ResourceLocationRegistry.BLOCK)
        @Comment("Build block by dimension: dimension ID = block ID")
        public Map<String, String> dimensionPlaceBlockIdList = linkedMap(List.of(
                Map.entry("minecraft:overworld", "minecraft:dirt"),
                Map.entry("minecraft:the_nether", "minecraft:netherrack"),
                Map.entry("minecraft:the_end", "minecraft:end_stone")
        ));

        @ResourceLocationPairMap
        @ResourceLocationSemantics(key = ResourceLocationRegistry.ENTITY, value = ResourceLocationRegistry.BLOCK)
        @Comment("Build block by mob: entity ID = block ID. Overrides dimension and fallback settings")
        public Map<String, String> mobPlaceBlockIdOverrideList = new LinkedHashMap<>();

        @ResourceLocationString
        @ResourceLocationSemantics(value = ResourceLocationRegistry.BLOCK)
        @Comment("Build block used when no mob or dimension-specific block is configured")
        public String fallbackPlaceBlockId = "minecraft:stone";

        @ResourceLocationPatternList
        @ResourceLocationSemantics(element = ResourceLocationRegistry.BLOCK)
        @Comment("Blocks that zombies will consider dangerous and attempt to cover or break. Supports wildcards and ! exclusions")
        public List<String> dangerousBlockIdList = new ArrayList<>(List.of(
                "minecraft:fire",
                "minecraft:soul_fire",
                "minecraft:campfire",
                "minecraft:soul_campfire",
                "minecraft:cactus",
                "minecraft:magma_block",
                "minecraft:sweet_berry_bush",
                "minecraft:wither_rose",
                "minecraft:powder_snow",
                "minecraft:lava",
                "minecraft:cobweb"
        ));

        private static <K, V> LinkedHashMap<K, V> linkedMap(List<Map.Entry<K, V>> entries)
        {
            LinkedHashMap<K, V> map = new LinkedHashMap<>();
            for (Map.Entry<K, V> entry : entries)
            {
                map.put(entry.getKey(), entry.getValue());
            }
            return map;
        }
    }

    public static class Ai
    {
        public Tactics tactics = new Tactics();

        @Comment("Affected mobs can see the nearest player no matter what.")
        public boolean alwaysSeeNearestPlayer = false;

        @Comment("Affected mobs can find a new target through blocks")
        public boolean canNoticeTargetsThroughBlocks = true;

        @Range(min = 0, max = 1000000)
        @Comment("(only if canNoticeTargetsThroughBlocks is true) Maximum solid blocks between a mob and a new target; 0 = unlimited")
        public int noticeTargetsThroughBlocksLimit = 3;

        @Comment("Zombies can keep chasing an already found target through blocks")
        public boolean canContinueSeeingTargetsThroughBlocks = true;

        @Range(min = 0, max = 1000000)
        @Comment("(only if canContinueSeeingTargetsThroughBlocks is true) Maximum solid blocks between a mob and its current target; 0 = unlimited")
        public int continueSeeingTargetsThroughBlocksLimit = 6;

        @ResourceLocationPatternList
        @ResourceLocationSemantics(element = ResourceLocationRegistry.ENTITY)
        @Comment("Mob IDs that get ZBB behavior. Supports wildcards, ! exclusions and @categories. (only Pathfinder mobs)")
        public List<String> affectedEntityIdList = new ArrayList<>(List.of("@monster"));

        @ResourceLocationPatternList
        @ResourceLocationSemantics(element = ResourceLocationRegistry.ENTITY)
        @Comment("Affected mob types that cannot place blocks. Supports wildcards, ! exclusions and @categories")
        public List<String> ignoreBuildEntityIdList = new ArrayList<>(List.of(
                "minecraft:ender_dragon",
                "minecraft:ghast",
                "minecraft:phantom",
                "minecraft:blaze",
                "minecraft:vex",
                "minecraft:elder_guardian",
                "minecraft:guardian",
                "minecraft:shulker",
                "minecraft:wither"
        ));

        @ResourceLocationPatternList
        @ResourceLocationSemantics(element = ResourceLocationRegistry.ENTITY)
        @Comment("Affected mob types that cannot break blocks. Supports wildcards, ! exclusions and @categories")
        public List<String> ignoreBreakEntityIdList = new ArrayList<>();

        public static class Tactics
        {
            @Comment("Allow mobs to climb toward higher targets by placing blocks beneath themselves")
            public boolean adjustHeightToTarget = true;

            @Comment("Allow mobs to build across gaps while moving toward a target")
            public boolean bridgeToTarget = true;

            @Comment("Allow mobs to break obstacles directly blocking their movement toward a target")
            public boolean clearObstaclesToTarget = true;

            @Comment("Allow mobs to cover or break dangerous blocks")
            public boolean mitigateDangerousBlocks = true;
        }
    }

    public static class Balance
    {
        @Range(min = 0, max = 1000000)
        @Comment("How long newly placed blocks are protected from being broken by ZBB mobs (seconds)")
        public double builtBlocksProtectionTime = 0.75D;

        @Range(min = 0, max = 16)
        @Comment("How far around a mob to search for dangerous blocks (blocks)")
        public int dangerousBlocksSearchRadius = 1;

        @Range(min = 1, max = 1000000)
        @Comment("Distance from an unreachable path end at which break/build tactics become high priority (blocks)")
        public int pathEndBreakBuildDistance = 6;

        @Range(min = 1, max = 1000000)
        @Comment("How long block damage data is stored (seconds)")
        public double damageStoreTime = 60.0D;

        public BlockDamage blockDamage = new BlockDamage();

        public Cooldowns cooldowns = new Cooldowns();

        public static class BlockDamage
        {
            @Range(min = 0, max = 1000000)
            @Comment("Maximum breakable vanilla block hardness; 0 = no limit.")
            public float maximumBreakableBlockHardness = 0.0f;

            @Range(min = 0, max = 1000000)
            @Comment("Base block damage, before tool and mob-size multipliers")
            public int damageToBlocks = 1;

            @Range(min = 0, max = 1000000)
            @Comment("Hardness exponent for block health: 1 = linear, <1 reduces differences, >1 increases them")
            public float blockHardnessContrast = 0.85f;

            @Range(min = 0, max = 1000000)
            @Comment("Multiplier applied to block health after the hardness exponent.")
            public float blockHardnessMultiplier = 2.0f;

            @ResourceLocationIntPairMap
            @ResourceLocationSemantics(key = ResourceLocationRegistry.BLOCK)
            @Comment("Per-block health overrides: block ID = health. Overrides the hardness-based calculation")
            public Map<String, Integer> blockHealthOverrideList = new LinkedHashMap<>();

            @Range(min = 0, max = 1000000)
            @Comment("Strength of held-tool damage scaling: 0 = ignore tools, 1 = full destroy-speed multiplier")
            public float itemDamageMultiplierStrength = 0.5f;

            @Range(min = 0, max = 1000000)
            @Comment("Strength of mob-size damage scaling relative to a zombie: 0 = ignore size, 1 = full volume ratio")
            public double hitboxSizeMultiplierStrength = 0.5;
        }

        public static class Cooldowns
        {
            @Range(min = 0, max = 1000000)
            @Comment("Delay between block-breaking actions (seconds)")
            public double breakCooldown = 1.0D;

            @Range(min = 0, max = 1000000)
            @Comment("Delay between block-placing actions (seconds)")
            public double buildCooldown = 1.0D;

            @Range(min = 0, max = 1000000)
            @Comment("Delay between searches for dangerous blocks (seconds)")
            public double searchDangerousBlocksCooldown = 1.0D;
        }
    }

    public static class BlockRestoration
    {
        @Comment("Enable disappearing for blocks placed by zombies")
        public boolean builtBlocksDisappearing = false;

        @Range(min = 0, max = 1000000)
        @Comment("(only if builtBlocksDisappearing is true) Time before blocks placed by zombies disappear (seconds)")
        public double builtBlocksDisappearTime = 30.0D;

        @Comment("Enable restoration of blocks broken by zombies")
        public boolean brokenBlocksRestoring = false;

        @Range(min = 0, max = 1000000)
        @Comment("(only if brokenBlocksRestoring is true) Time before blocks broken by zombies are restored (seconds)")
        public double brokenBlocksRestoreTime = 30.0D;
    }

    public static class VisualEffects
    {
        @Comment("[Break] Play the mob's swing animation when damaging or breaking a block")
        public boolean breakMobSwing = true;

        @Comment("[Build] Play the block's placement sound when a mob places it")
        public boolean buildBlockSound = true;

        @Comment("[BrokenReappear]Show assembling block particles shortly before restoration")
        public boolean brokenReappearParticles = true;

        @Comment("[BrokenReappear]Show a periodic marker where a broken block is waiting to be restored")
        public boolean brokenReappearMarkerParticle = true;

        @Comment("[BrokenReappear] Play a charge-up sound shortly before restoration")
        public boolean brokenReappearChargeSound = true;

        @Comment("[BrokenReappear] Play a sound when the broken block is restored")
        public boolean brokenReappearSound = true;

        @Comment("[BuiltDisappear] Show a shrinking block animation when a mob-placed block disappears")
        public boolean builtDisappearBlockDisplay = true;

        @Comment("[BuiltDisappear] Play the shrink sound when a mob-placed block starts disappearing")
        public boolean builtDisappearShrinkSound = true;

        @Comment("[BuiltDisappear] Play the final sound after a mob-placed block disappears")
        public boolean builtDisappearSound = true;
    }
}
