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
    public BlockReturning blockReturning = new BlockReturning();
    public Audio audio = new Audio();

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
        @Comment("The block that zombies will place")
        public String bridgeBlockId = "minecraft:dirt";

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
        @Comment("Zombies will see the nearest player no matter what.")
        public boolean alwaysSeeNearestPlayer = false;

        @Comment("Zombies will be able to see targets through walls")
        public boolean canSeeTargetsThroughBlocks = true;

        @Comment("Gives all monsters, not just zombies, the ability to break and place blocks")
        public boolean applyToAllMonsters = true;

        @ResourceLocationList
        @Comment("Additional entity IDs that will be given the ability to break and place blocks")
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
        @Comment("Freeze time after breaking or placing a block (seconds)")
        public double freezeTime = 0.5D;

        @Range(min = 0, max = 1000000)
        @Comment("Time a zombie must be stuck before it start breaking or placing blocks (seconds)")
        public double stuckSecondsBeforeBreakAndBuild = 3.0D;

        @Range(min = 0, max = 1000000)
        @Comment("Time during which zombies cannot break blocks they just placed (seconds)")
        public double builtBlocksProtectionTime = 0.75D;

        @Range(min = 0, max = 1000000)
        @Comment("The radius within which zombies will detect dangerous blocks")
        public int dangerousBlocksSearchRadius = 1;

        public BlockDamage blockDamage = new BlockDamage();

        public Cooldowns cooldowns = new Cooldowns();

        public Optimization optimization = new Optimization();

        public static class BlockDamage
        {
            @Range(min = 1, max = 1000000)
            @Comment("Damage dealt to blocks")
            public int damageToBlocks = 3;

            @Range(min = 0, max = 1000000)
            @Comment("How strongly the tool-in-hand damage multiplier affects block damage (0 = tools do not affect damage)")
            public float itemDamageMultiplierStrength = 0.5f;

            @Range(min = 0, max = 1000000)
            @Comment("How strongly the hitbox size multiplier affects block damage (0 = hitbox size does not affect damage)")
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
        }

        public static class Optimization
        {
            @Range(min = 0.05, max = 1000000)
            @Comment("Interval between moving toward the target (seconds)")
            public double goToTargetInterval = 0.5D;

            @Range(min = 0.05, max = 1000000)
            @Comment("Interval between path recalculations (seconds)")
            public double pathCheckInterval = 2.0D;

            @Range(min = 0, max = 1000000)
            @Comment("Distance in blocks below which the distance-based cooldown multiplier is not applied to goToTarget/pathCheck intervals")
            public double distanceCooldownStartBlocks = 5.0D;

            @Range(min = 0, max = 1000000)
            @Comment("Distance in blocks at which the cooldown multiplier for goToTarget/pathCheck-intervals reaches its maximum")
            public double distanceCooldownMaxBlocks = 64.0D;

            @Range(min = 1.0, max = 1000000)
            @Comment("Maximum distance-based multiplier for goToTarget/pathCheck intervals")
            public double distanceCooldownMaxMultiplier = 8.0D;

            @Range(min = 1, max = 1000000)
            @Comment("How long block damage data is stored (seconds)")
            public double damageStoreTime = 60.0D;

            @Range(min = 0.05, max = 1000000)
            @Comment("Interval between searches for dangerous blocks (seconds)")
            public double searchDangerousBlocksInterval = 1.0D;
        }
    }

    public static class BlockReturning
    {
        @Comment("Enable disappearing of blocks placed by zombies")
        public boolean builtBlocksDisappearing = false;

        @Range(min = 0, max = 1000000)
        @Comment("Time after which blocks placed by zombies disappear (only if builtBlocksDisappearing is true)")
        public double builtBlocksDisappearTime = 30.0D;

        @Comment("Enable restoration of blocks broken by zombies")
        public boolean brokenBlocksRestoring = false;

        @Range(min = 0, max = 1000000)
        @Comment("Time after which blocks broken by zombies are restored (only if brokenBlocksRestoring is true)")
        public double brokenBlocksRestoreTime = 30.0D;
    }

    public static class Audio
    {
        @Range(min = 0, max = 1000000)
        @Comment("Volume multiplier for the sound played when placing a block")
        public float placeSoundVolumeMultiplier = 1f;
    }
}
