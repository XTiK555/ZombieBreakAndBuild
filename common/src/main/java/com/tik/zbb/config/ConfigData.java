package com.tik.zbb.config;

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
    public transient Set<Identifier> ignoreHostileEntityIdSet = Set.of();
    public transient Set<Identifier> additionalEntityIdSet = Set.of();

    public void rebuildSets()
    {
        dangerousBlockIdSet = idListToSet(blocks.dangerousBlockIdList);
        ignoreHostileEntityIdSet = idListToSet(ai.ignoreEntityIdList);
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
        @Comment("The block that zombies will place")
        public String bridgeBlockId = "minecraft:dirt";

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

        @Comment("Additional IDs of entities that will be given the ability to break and place blocks")
        public List<String> additionalEntityIdList = new ArrayList<>();

        @Comment("IDs of entities that will NOT be given the ability to break and place blocks")
        public List<String> ignoreEntityIdList = new ArrayList<>();

        @Comment("Target search radius")
        public int targetSearchRadius = 35;

        @Comment("The radius within which zombies will see dangerous blocks")
        public int dangerousBlocksSearchRadius = 1;
    }

    public static class Balance
    {
        @Comment("Cooldown between block breaking attempts (seconds)")
        public double breakCooldown = 1.0D;

        @Comment("Cooldown between block placing attempts (seconds)")
        public double buildCooldown = 1.0D;

        @Comment("Damage dealt to blocks")
        public int damageToBlocks = 3;

        @Comment("Freeze time after break/place block in seconds")
        public double freezeTime = 0.5D;

        @Comment("Interval between searches for dangerous blocks (seconds)")
        public double searchDangerousBlocksInterval = 1.0D;

        @Comment("Go to target interval in seconds")
        public double goToTargetInterval = 0.5D;

        @Comment("Interval between path recalculations (seconds)")
        public double pathCheckInterval = 2.0D;

        @Comment("Time stuck before zombies start breaking or placing blocks (seconds)")
        public double stuckSecondsBeforeBreakAndBuild = 3.0D;

        @Comment("Duration damage data for blocks is stored (seconds)")
        public double damageStoreTime = 60.0D;

        @Comment("Time during which zombies cannot break blocks they just placed (seconds)")
        public double builtBlocksProtectionTime = 0.75D;
    }

    public static class Audio
    {
        @Comment("Sound played when placing a block")
        public String placeSoundId = "minecraft:block.rooted_dirt.place";

        @Comment("Sound played when hitting a block")
        public String hitSoundId = "minecraft:entity.zombie.attack_wooden_door";

        @Comment("Sound played when breaking a block")
        public String breakSoundId = "minecraft:entity.zombie.break_wooden_door";
    }
}
