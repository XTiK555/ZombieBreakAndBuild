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
        @Comment("Dimension-specific blocks used when mobs build")
        public Map<String, String> dimensionPlaceBlockIdList = linkedMap(List.of(
                Map.entry("minecraft:overworld", "minecraft:dirt"),
                Map.entry("minecraft:the_nether", "minecraft:netherrack"),
                Map.entry("minecraft:the_end", "minecraft:end_stone")
        ));

        @ResourceLocationPairMap
        @ResourceLocationSemantics(key = ResourceLocationRegistry.ENTITY, value = ResourceLocationRegistry.BLOCK)
        @Comment("Mob-specific build block overrides. These have priority over dimensionPlaceBlockIdList.")
        public Map<String, String> mobPlaceBlockIdOverrideList = new LinkedHashMap<>();

        @ResourceLocationString
        @ResourceLocationSemantics(value = ResourceLocationRegistry.BLOCK)
        @Comment("Block used when no mob-specific or dimension-specific build block is configured")
        public String fallbackPlaceBlockId = "minecraft:stone";

        @ResourceLocationPatternList
        @ResourceLocationSemantics(element = ResourceLocationRegistry.BLOCK)
        @Comment("Blocks that zombies will consider dangerous and attempt to cover or break")
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

        @Comment("Zombies can see the nearest player no matter what.")
        public boolean alwaysSeeNearestPlayer = false;

        @Comment("Zombies can find a new target through blocks")
        public boolean canNoticeTargetsThroughBlocks = true;

        @Range(min = 0, max = 1000000)
        @Comment("(only if canNoticeTargetsThroughBlocks is true) How many solid blocks can be between zombie and target when noticing through walls (0 - infinity)")
        public int noticeTargetsThroughBlocksLimit = 3;

        @Comment("Zombies can keep chasing an already found target through blocks")
        public boolean canContinueSeeingTargetsThroughBlocks = true;

        @Range(min = 0, max = 1000000)
        @Comment("(only if canContinueSeeingTargetsThroughBlocks is true) How many solid blocks can be between zombie and target when continuing to see through walls (0 - infinity)")
        public int continueSeeingTargetsThroughBlocksLimit = 6;

        @ResourceLocationPatternList
        @ResourceLocationSemantics(element = ResourceLocationRegistry.ENTITY)
        @Comment("Entity ID patterns that will be given Zombie Break & Build behavior (only Pathfinder mobs)")
        public List<String> affectedEntityIdList = new ArrayList<>(List.of("*:*"));

        @ResourceLocationPatternList
        @ResourceLocationSemantics(element = ResourceLocationRegistry.ENTITY)
        @Comment("Entity ID patterns that will NOT be given the ability to place blocks")
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
        @Comment("Entity ID patterns that will NOT be given the ability to break blocks")
        public List<String> ignoreBreakEntityIdList = new ArrayList<>();

        public static class Tactics
        {
            @Comment("Whether mobs can adjust their height to reach a target")
            public boolean adjustHeightToTarget = true;

            @Comment("Whether mobs can build bridges toward a target")
            public boolean bridgeToTarget = true;

            @Comment("Whether mobs can clear obstacles between themselves and a target")
            public boolean clearObstaclesToTarget = true;

            @Comment("Whether mobs can cover or break dangerous blocks")
            public boolean mitigateDangerousBlocks = true;
        }
    }

    public static class Balance
    {
        @Range(min = 0, max = 1000000)
        @Comment("Time during which zombies cannot break blocks they just placed (seconds)")
        public double builtBlocksProtectionTime = 0.75D;

        @Range(min = 0, max = 16)
        @Comment("The radius within which zombies will detect dangerous blocks")
        public int dangerousBlocksSearchRadius = 1;

        @Range(min = 1, max = 1000000)
        @Comment("How close a mob must get to the end of its path before it tries to break or place blocks")
        public int pathEndBreakBuildDistance = 6;

        @Range(min = 1, max = 1000000)
        @Comment("How long block damage data is stored (seconds)")
        public double damageStoreTime = 60.0D;

        public BlockDamage blockDamage = new BlockDamage();

        public Cooldowns cooldowns = new Cooldowns();

        public static class BlockDamage
        {
            @Range(min = 0, max = 1000000)
            @Comment("Maximum vanilla block hardness mobs can break (0 - infinity)")
            public float maximumBreakableBlockHardness = 0.0f;

            @Range(min = 0, max = 1000000)
            @Comment("Damage dealt to blocks")
            public int damageToBlocks = 1;

            @Range(min = 0, max = 1000000)
            @Comment("Exponent applied to vanilla block hardness. 1 = no contrast change.")
            public float blockHardnessContrast = 0.85f;

            @Range(min = 0, max = 1000000)
            @Comment("Multiplier applied to the calculated block health after hardness contrast is applied.")
            public float blockHardnessMultiplier = 2.0f;

            @ResourceLocationIntPairMap
            @ResourceLocationSemantics(key = ResourceLocationRegistry.BLOCK)
            @Comment("Manual block health overrides")
            public Map<String, Integer> blockHealthOverrideList = new LinkedHashMap<>();

            @Range(min = 0, max = 1000000)
            @Comment("How strongly the tool-in-hand damage multiplier affects block damage (0 - tools do not affect damage)")
            public float itemDamageMultiplierStrength = 0.5f;

            @Range(min = 0, max = 1000000)
            @Comment("How strongly the hitbox size multiplier affects block damage (0 - hitbox size does not affect damage)")
            public double hitboxSizeMultiplierStrength = 0.5;
        }

        public static class Cooldowns
        {
            @Range(min = 0, max = 1000000)
            @Comment("Cooldown between block breaking attempts (seconds)")
            public double breakCooldown = 1.0D;

            @Range(min = 0, max = 1000000)
            @Comment("Cooldown between block placing attempts (seconds)")
            public double buildCooldown = 1.0D;

            @Range(min = 0, max = 1000000)
            @Comment("Cooldown between searches for dangerous blocks (seconds)")
            public double searchDangerousBlocksCooldown = 1.0D;
        }
    }

    public static class BlockRestoration
    {
        @Comment("Enable disappearing for blocks placed by zombies")
        public boolean builtBlocksDisappearing = false;

        @Range(min = 0, max = 1000000)
        @Comment("Time before blocks placed by zombies disappear (only if builtBlocksDisappearing is true)")
        public double builtBlocksDisappearTime = 30.0D;

        @Comment("Enable restoration of blocks broken by zombies")
        public boolean brokenBlocksRestoring = false;

        @Range(min = 0, max = 1000000)
        @Comment("Time before blocks broken by zombies are restored (only if brokenBlocksRestoring is true)")
        public double brokenBlocksRestoreTime = 30.0D;
    }

    public static class VisualEffects
    {
        @Comment("[Break] Whether the zombie will swing when breaking a block")
        public boolean breakMobSwing = true;

        @Comment("[Build] Whether a placed block will play a sound")
        public boolean buildBlockSound = true;

        @Comment("[BrokenReappear] Whether a reappearing broken block will spawn particles")
        public boolean brokenReappearParticles = true;

        @Comment("[BrokenReappear] Whether to show a marker particle at the future reappearance position")
        public boolean brokenReappearMarkerParticle = true;

        @Comment("[BrokenReappear] Whether a reappearing broken block will play a charge sound")
        public boolean brokenReappearChargeSound = true;

        @Comment("[BrokenReappear] Whether a reappearing broken block will play a sound")
        public boolean brokenReappearSound = true;

        @Comment("[BuiltDisappear] Whether a disappearing built block will spawn a block display")
        public boolean builtDisappearBlockDisplay = true;

        @Comment("[BuiltDisappear] Whether a disappearing built block will play a shrink sound")
        public boolean builtDisappearShrinkSound = true;

        @Comment("[BuiltDisappear] Whether a disappearing built block will play a sound")
        public boolean builtDisappearSound = true;
    }
}
