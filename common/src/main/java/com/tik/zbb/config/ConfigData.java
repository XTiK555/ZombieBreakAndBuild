package com.tik.zbb.config;

import net.minecraft.resources.Identifier;

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
            "minecraft:powder_snow"
    ));
    public List<String> impassableBlockIdList = new ArrayList<>(List.of(
            "minecraft:cobweb",
            "minecraft:pointed_dripstone"
    ));

    // ======================
    // [AI / BEHAVIOR]
    // ======================
    public Boolean isAlwaysSeeNearestPlayer = false;
    public Boolean isApplyingToAllHostiles = true;
    public Boolean isAttackingAllEntities = false;
    public Boolean isCanSeeTargetsThroughBlocks = true;

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
    // [Runtime caches (не сериализуются)]
    // ======================
    public transient Set<Identifier> dangerousBlocksSet = Set.of();
    public transient Set<Identifier> impassableBlocksSet = Set.of();

    public void rebuildSets()
    {
        dangerousBlocksSet = parseListToSet(dangerousBlockIdList);
        impassableBlocksSet = parseListToSet(impassableBlockIdList);
    }

    private Set<Identifier> parseListToSet(List<String> list)
    {
        Set<Identifier> set = new HashSet<>();
        for (String s : list)
        {
            if (s == null || s.isBlank()) continue;

            Identifier id = Identifier.tryParse(s);
            if (id != null) set.add(id);
        }
        return Set.copyOf(set);
    }
}
