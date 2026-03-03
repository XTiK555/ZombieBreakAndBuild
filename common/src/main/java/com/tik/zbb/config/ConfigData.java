package com.tik.zbb.config;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConfigData
{
    // ======================
    // [BLOCKS]
    // ======================
    public String bridgeBlockId = "minecraft:dirt";
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

    // ======================
    // [AI / BEHAVIOR]
    // ======================
    public Boolean alwaysSeeNearestPlayer = false;
    public Boolean canSeeTargetsThroughBlocks = true;
    public Boolean applyToAllMonsters = true;
    public List<String> additionalEntityIdList = new ArrayList<>(List.of());
    public List<String> ignoreEntityIdList = new ArrayList<>(List.of());

    public Integer targetSearchRadius = 35;
    public Integer dangerousBlocksSearchRadius = 1;

    // ======================
    // [BALANCE / TIMERS]
    // ======================
    public Double breakCooldown = 1.0D;
    public Double buildCooldown = 1.0D;
    public Integer damageToBlocks = 3;

    public Double freezeTime = 0.5D;
    public Double searchDangerousBlocksInterval = 1.0D;
    public Double goToTargetInterval = 0.5D;
    public Double pathCheckInterval = 2.0D;

    public Double stuckSecondsBeforeBreakAndBuild = 3.0D;
    public Double damageStoreTime = 60.0D;
    public Double builtBlocksProtectionTime = 0.75D;

    // ======================
    // [AUDIO]
    // ======================
    public String placeSoundId = "minecraft:block.rooted_dirt.place";
    public String hitSoundId = "minecraft:entity.zombie.attack_wooden_door";
    public String breakSoundId = "minecraft:entity.zombie.break_wooden_door";

    // ======================
    // [Runtime caches]
    // ======================
    public transient Set<ResourceLocation> dangerousBlockIdSet = Set.of();
    public transient Set<ResourceLocation> ignoreHostileEntityIdSet = Set.of();
    public transient Set<ResourceLocation> additionalEntityIdSet = Set.of();

    public void rebuildSets()
    {
        dangerousBlockIdSet = parseListToSet(dangerousBlockIdList);
        additionalEntityIdSet = parseListToSet(additionalEntityIdList);
        ignoreHostileEntityIdSet = parseListToSet(ignoreEntityIdList);
    }

    private Set<ResourceLocation> parseListToSet(List<String> list)
    {
        Set<ResourceLocation> set = new HashSet<>();
        for (String s : list)
        {
            if (s == null || s.isBlank()) continue;

            ResourceLocation id = ResourceLocation.tryParse(s);
            if (id != null) set.add(id);
        }
        return Set.copyOf(set);
    }
}
