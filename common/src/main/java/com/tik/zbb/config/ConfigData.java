package com.tik.zbb.config;

import com.tik.zbb.config.annotations.*;
import net.minecraft.resources.Identifier;

import java.util.*;

public class ConfigData
{
    public Blocks blocks = new Blocks();
    public Ai ai = new Ai();
    public Balance balance = new Balance();
    public BlockRestoration blockRestoration = new BlockRestoration();
    public VisualEffects visualEffects = new VisualEffects();

    public transient Set<Identifier> dangerousBlockIdSet = Set.of();
    public transient Set<Identifier> ignoreBuildEntityIdSet = Set.of();
    public transient Set<Identifier> ignoreBreakEntityIdSet = Set.of();
    public transient Set<Identifier> additionalEntityIdSet = Set.of();
    public transient Map<Identifier, Identifier> dimensionPlaceBlockIdMap = Map.of();
    public transient Map<Identifier, Identifier> mobPlaceBlockIdOverrideMap = Map.of();

    public void rebuildSets()
    {
        dangerousBlockIdSet = idListToSet(blocks.dangerousBlockIdList);
        ignoreBuildEntityIdSet = idListToSet(ai.ignoreBuildEntityIdList);
        ignoreBreakEntityIdSet = idListToSet(ai.ignoreBreakEntityIdList);
        additionalEntityIdSet = idListToSet(ai.additionalEntityIdList);
        dimensionPlaceBlockIdMap = idPairListToMap(blocks.dimensionPlaceBlockIdList);
        mobPlaceBlockIdOverrideMap = idPairListToMap(blocks.mobPlaceBlockIdOverrideList);
    }

    private static Set<Identifier> idListToSet(List<String> list)
    {
        Set<Identifier> set = new HashSet<>();

        for (String s : list)
        {
            Identifier id = Identifier.tryParse(s);
            if (id != null) set.add(id);
        }

        return Set.copyOf(set);
    }

    private static Map<Identifier, Identifier> idPairListToMap(List<String> list)
    {
        Map<Identifier, Identifier> map = new HashMap<>();

        for (String s : list)
        {
            String[] parts = s.split("=", 2);
            if (parts.length != 2) continue;

            Identifier key = Identifier.tryParse(parts[0].trim());
            Identifier value = Identifier.tryParse(parts[1].trim());
            if (key != null && value != null) map.put(key, value);
        }

        return Map.copyOf(map);
    }

    public static class Blocks
    {
        @ResourceLocationString
        @Comment("Block used when no dimension-specific or mob-specific build block is configured")
        public String fallbackPlaceBlockId = "minecraft:stone";

        @ResourceLocationPairList
        @Comment("Dimension-specific blocks used when mobs build (dimensionId=blockId)")
        public List<String> dimensionPlaceBlockIdList = new ArrayList<>(List.of(
                "minecraft:overworld=minecraft:dirt",
                "minecraft:the_nether=minecraft:netherrack",
                "minecraft:the_end=minecraft:end_stone"
        ));

        @ResourceLocationPairList
        @Comment("Mob-specific build block overrides, for example: \"minecraft:zombie=stone\", \"minecraft:pig=minecraft:dirt\"")
        public List<String> mobPlaceBlockIdOverrideList = new ArrayList<>();

        @ResourceLocationList
        @Comment("Blocks that zombies will consider dangerous and attempt to build on or break")
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
    }

    public static class Ai
    {
        @Comment("Zombies can see the nearest player no matter what.")
        public boolean alwaysSeeNearestPlayer = false;

        @Comment("Zombies can notice targets through walls")
        public boolean canNoticeTargetsThroughBlocks = true;

        @Range(min = 0, max = 1000000)
        @Comment("(only if canNoticeTargetsThroughBlocks is true) How many solid blocks can be between zombie and target when noticing through walls (0 - infinity)")
        public int noticeTargetsThroughBlocksLimit = 3;

        @Comment("Zombies can continue seeing targets through walls")
        public boolean canContinueSeeingTargetsThroughBlocks = true;

        @Range(min = 0, max = 1000000)
        @Comment("(only if canContinueSeeingTargetsThroughBlocks is true) How many solid blocks can be between zombie and target when continuing to see through walls (0 - infinity)")
        public int continueSeeingTargetsThroughBlocksLimit = 6;

        @Comment("Gives all monsters, not just zombies, the ability to break and place blocks")
        public boolean applyToAllMonsters = true;

        @ResourceLocationList
        @Comment("Additional entity IDs that will be given the ability to break and place blocks (only Pathfinder mobs)")
        public List<String> additionalEntityIdList = new ArrayList<>();

        @ResourceLocationList
        @Comment("Entity IDs that will NOT be given the ability to place blocks")
        public List<String> ignoreBuildEntityIdList = new ArrayList<>(List.of(
                "minecraft:ender_dragon",
                "minecraft:ghast",
                "minecraft:phantom",
                "minecraft:blaze",
                "minecraft:vex",
                "minecraft:elder_guardian",
                "minecraft:guardian",
                "minecraft:shulker",
                "minecraft:wither",
                "minecraft:breeze"
        ));

        @ResourceLocationList
        @Comment("Entity IDs that will NOT be given the ability to break blocks")
        public List<String> ignoreBreakEntityIdList = new ArrayList<>(List.of(
                "minecraft:ender_dragon",
                "minecraft:shulker",
                "minecraft:wither",
                "minecraft:vex"
        ));
    }

    public static class Balance
    {
        @Range(min = 0, max = 1000000)
        @Comment("Time during which zombies cannot break blocks they just placed (seconds)")
        public double builtBlocksProtectionTime = 0.75D;

        @Range(min = 0, max = 1000000)
        @Comment("The radius within which zombies will detect dangerous blocks")
        public int dangerousBlocksSearchRadius = 1;

        @Range(min = 1, max = 1000000)
        @Comment("Distance to path end before breaking or building")
        public int pathEndBreakBuildDistance = 6;

        @Range(min = 1, max = 1000000)
        @Comment("How long block damage data is stored (seconds)")
        public double damageStoreTime = 60.0D;

        public BlockDamage blockDamage = new BlockDamage();

        public Cooldowns cooldowns = new Cooldowns();

        public static class BlockDamage
        {
            @Range(min = 1, max = 1000000)
            @Comment("Damage dealt to blocks")
            public int damageToBlocks = 3;

            @Range(min = 0, max = 1000000)
            @Comment("How strongly the tool-in-hand damage multiplier affects block damage (0 - tools do not affect damage)")
            public float itemDamageMultiplierStrength = 0.5f;

            @Range(min = 0, max = 1000000)
            @Comment("How strongly the hitbox size multiplier affects block damage (0 - hitbox size does not affect damage)")
            public double hitboxSizeMultiplierStrength = 0.5;
        }

        public static class Cooldowns
        {
            @Range(min = 0.05, max = 1000000)
            @Comment("Cooldown between block breaking attempts (seconds)")
            public double breakCooldown = 1.0D;

            @Range(min = 0.05, max = 1000000)
            @Comment("Cooldown between block placing attempts (seconds)")
            public double buildCooldown = 1.0D;

            @Range(min = 0.05, max = 1000000)
            @Comment("Cooldown between searches for dangerous blocks (seconds)")
            public double searchDangerousBlocksCooldown = 1.0D;
        }
    }

    public static class BlockRestoration
    {
        @Comment("Enable disappearing of blocks placed by zombies")
        public boolean builtBlocksDisappearing = false;

        @Range(min = 0, max = 1000000)
        @Comment("Time before placed blocks by zombies disappear (only if builtBlocksDisappearing is true)")
        public double builtBlocksDisappearTime = 30.0D;

        @Comment("Enable restoration of blocks broken by zombies")
        public boolean brokenBlocksRestoring = false;

        @Range(min = 0, max = 1000000)
        @Comment("Time before broken blocks by zombies are restored (only if brokenBlocksRestoring is true)")
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
