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
    public Audio audio = new Audio();

    public transient Set<ResourceLocation> dangerousBlockIdSet = Set.of();
    public transient Set<ResourceLocation> ignoreHostileEntityIdSet = Set.of();
    public transient Set<ResourceLocation> additionalEntityIdSet = Set.of();

    public void rebuildSets()
    {
        dangerousBlockIdSet = idListToSet(blocks.dangerousBlockIdList);
        ignoreHostileEntityIdSet = idListToSet(ai.ignoreEntityIdList);
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
                "minecraft:lava"
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
        @Comment("IDs of entities that will NOT be given the ability to break and place blocks")
        public List<String> ignoreEntityIdList = new ArrayList<>();

        @Range(min = 0, max = 1000000)
        @Comment("Target search radius")
        public int targetSearchRadius = 35;

        @Range(min = 0, max = 1000000)
        @Comment("The radius within which zombies will see dangerous blocks")
        public int dangerousBlocksSearchRadius = 1;
    }

    public static class Balance
    {
        @Range(min = 0.05, max = 1000000)
        @Comment("Cooldown between block breaking attempts (seconds)")
        public double breakCooldown = 1.0D;

        @Range(min = 0.05, max = 1000000)
        @Comment("Cooldown between block placing attempts (seconds)")
        public double buildCooldown = 1.0D;

        @Range(min = 1, max = 1000000)
        @Comment("Damage dealt to blocks")
        public int damageToBlocks = 3;

        @Comment("Will block damage scale depending on hitbox size")
        public boolean scaleDamageToBlocksWithHitbox = true;

        @Range(min = 0, max = 1000000)
        @Comment("Freeze time after break/place block in seconds")
        public double freezeTime = 0.5D;

        @Range(min = 0.05, max = 1000000)
        @Comment("Interval between searches for dangerous blocks (seconds)")
        public double searchDangerousBlocksInterval = 1.0D;

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

        @Range(min = 0, max = 1000000)
        @Comment("Time stuck before zombies start breaking or placing blocks (seconds)")
        public double stuckSecondsBeforeBreakAndBuild = 3.0D;

        @Range(min = 1, max = 1000000)
        @Comment("Duration damage data for blocks is stored (seconds)")
        public double damageStoreTime = 60.0D;

        @Range(min = 0, max = 1000000)
        @Comment("Time during which zombies cannot break blocks they just placed (seconds)")
        public double builtBlocksProtectionTime = 0.75D;
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
