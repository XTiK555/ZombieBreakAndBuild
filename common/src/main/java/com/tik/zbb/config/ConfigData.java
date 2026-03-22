package com.tik.zbb.config;

import com.tik.zbb.config.annotations.Comment;
import com.tik.zbb.config.annotations.Range;
import com.tik.zbb.config.annotations.ResourceLocationList;
import com.tik.zbb.config.annotations.ResourceLocationString;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConfigData
{
    public Blocks blocks = new Blocks();
    public Ai ai = new Ai();
    public Balance balance = new Balance();
    public BlockRestoration blockRestoration = new BlockRestoration();
    public VisualEffects visualEffects = new VisualEffects();

    public transient Set<ResourceLocation> dangerousBlockIdSet = Set.of();
    public transient Set<ResourceLocation> ignoreBuildEntityIdSet = Set.of();
    public transient Set<ResourceLocation> ignoreBreakEntityIdSet = Set.of();
    public transient Set<ResourceLocation> additionalEntityIdSet = Set.of();

    public void rebuildSets()
    {
        dangerousBlockIdSet = idListToSet(blocks.dangerousBlockIdList);
        ignoreBuildEntityIdSet = idListToSet(ai.ignoreBuildEntityIdList);
        ignoreBreakEntityIdSet = idListToSet(ai.ignoreBreakEntityIdList);
        additionalEntityIdSet = idListToSet(ai.additionalEntityIdList);
    }

    private static Set<ResourceLocation> idListToSet(List<String> list)
    {
        Set<ResourceLocation> set = new HashSet<>();

        for (String s : list)
        {
            ResourceLocation id = ResourceLocation.tryParse(s);
            if (id != null) set.add(id);
        }

        return Set.copyOf(set);
    }

    public static class Blocks
    {
        @ResourceLocationString
        @Comment("Block used when mobs build")
        public String placeBlockId = "minecraft:dirt";

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

        @Comment("Zombies can see targets through walls (only for mobs that use NearestAttackableTargetGoal)")
        public boolean canSeeTargetsThroughBlocks = true;

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
        @Comment("[Break] Whether the mob will swing when breaking a block")
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
