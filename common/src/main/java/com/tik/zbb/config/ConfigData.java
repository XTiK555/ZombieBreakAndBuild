package com.tik.zbb.config;

import com.tik.zbb.config.annotations.Comment;
import com.tik.zbb.config.annotations.Range;
import com.tik.zbb.config.annotations.ResourceLocationList;
import com.tik.zbb.config.annotations.ResourceLocationString;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConfigData
{
    public Blocks blocks = new Blocks();
    public Ai ai = new Ai();
    public Balance balance = new Balance();
    public Audio audio = new Audio();

    public transient Set<Identifier> dangerousBlockIdSet = Set.of();
    public transient Set<Identifier> ignoreBuildEntityIdSet = Set.of();
    public transient Set<Identifier> ignoreBreakEntityIdSet = Set.of();
    public transient Set<Identifier> additionalEntityIdSet = Set.of();

    public void rebuildSets()
    {
        dangerousBlockIdSet = idListToSet(blocks.dangerousBlockIdList);
        ignoreBuildEntityIdSet = idListToSet(ai.ignoreBuildEntityIdList);
        ignoreBreakEntityIdSet = idListToSet(ai.ignoreBreakEntityIdList);
        additionalEntityIdSet = idListToSet(ai.additionalEntityIdList);
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

    public static class Blocks
    {
        @ResourceLocationString
        @Comment("The block that zombies will place")
        public String bridgeBlockId = "minecraft:dirt";

        @ResourceLocationList
        @Comment("Blocks that zombies will consider dangerous (attempt to build on/break)")
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

        @Comment("Give the ability to break and place blocks to all monsters, not just zombies")
        public boolean applyToAllMonsters = true;

        @ResourceLocationList
        @Comment("Additional IDs of entities that will be given the ability to break and place blocks")
        public List<String> additionalEntityIdList = new ArrayList<>();

        @ResourceLocationList
        @Comment("IDs of entities that will NOT be given the ability to place blocks")
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
        @Comment("IDs of entities that will NOT be given the ability to break blocks")
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
        @Comment("Freeze time after break/place block in seconds")
        public double freezeTime = 0.5D;

        @Range(min = 0, max = 1000000)
        @Comment("Time stuck before zombies start breaking or placing blocks (seconds)")
        public double stuckSecondsBeforeBreakAndBuild = 3.0D;

        @Range(min = 0, max = 1000000)
        @Comment("Time during which zombies cannot break blocks they just placed (seconds)")
        public double builtBlocksProtectionTime = 0.75D;

        @Range(min = 0, max = 1000000)
        @Comment("The radius within which zombies will see dangerous blocks")
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
            @Comment("How much will the block damage multiplier from the tool in hand affect? (0 - the tool has no effect to damage)")
            public float itemDamageMultiplierStrength = 0.5f;

            @Range(min = 0, max = 1000000)
            @Comment("How much will the block damage multiplier from the hitbox size? (0 - the hitbox size has no effect to damage)")
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
            @Comment("Go to target interval in seconds")
            public double goToTargetInterval = 0.5D;

            @Range(min = 0.05, max = 1000000)
            @Comment("Interval between path recalculations (seconds)")
            public double pathCheckInterval = 2.0D;

            @Range(min = 0, max = 1000000)
            @Comment("Distance in blocks up to which distance multiplier for goToTarget/pathCheck-intervals is not applied")
            public double distanceCooldownStartBlocks = 5.0D;

            @Range(min = 0, max = 1000000)
            @Comment("Distance in blocks at which the cooldown multiplier for goToTarget/pathCheck-intervals reaches its maximum")
            public double distanceCooldownMaxBlocks = 64.0D;

            @Range(min = 1.0, max = 1000000)
            @Comment("Maximum multiplier for goToTarget/pathCheck-intervals based on distance to target")
            public double distanceCooldownMaxMultiplier = 8.0D;

            @Range(min = 1, max = 1000000)
            @Comment("Duration damage data for blocks is stored (seconds)")
            public double damageStoreTime = 60.0D;

            @Range(min = 0.05, max = 1000000)
            @Comment("Interval between searches for dangerous blocks (seconds)")
            public double searchDangerousBlocksInterval = 1.0D;
        }
    }

    public static class Audio
    {
        @Range(min = 0, max = 1000000)
        @Comment("Sound played when placing a block")
        public float placeSoundVolumeMultiplier = 1f;

        @Range(min = 0, max = 1000000)
        @Comment("Sound played when hitting a block")
        public float hitSoundVolumeMultiplier = 1f;

        @Range(min = 0, max = 1000000)
        @Comment("Sound played when breaking a block")
        public float breakSoundVolumeMultiplier = 1f;
    }
}
