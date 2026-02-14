package com.tik.zbb;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class Config
{
    public static final ForgeConfigSpec SPEC;

    // Public config values
    public static final ForgeConfigSpec.ConfigValue<String> BRIDGE_BLOCK_ID;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> DANGEROUS_BLOCKS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> IMPASSABLE_BLOCKS;

    public static final ForgeConfigSpec.BooleanValue ALWAYS_SEE_NEAREST_PLAYER;
    public static final ForgeConfigSpec.BooleanValue APPLY_TO_ALL_HOSTILES;
    public static final ForgeConfigSpec.BooleanValue ATTACK_ALL_ENTITIES;
    public static final ForgeConfigSpec.IntValue TARGET_SEARCH_RADIUS;
    public static final ForgeConfigSpec.IntValue DANGEROUS_BLOCKS_SCAN_RADIUS;

    public static final ForgeConfigSpec.DoubleValue BREAK_COOLDOWN;
    public static final ForgeConfigSpec.DoubleValue BUILD_COOLDOWN;
    public static final ForgeConfigSpec.IntValue DAMAGE_TO_BLOCKS;
    public static final ForgeConfigSpec.DoubleValue FREEZE_TIME;
    public static final ForgeConfigSpec.DoubleValue SEARCH_DANGEROUS_INTERVAL;
    public static final ForgeConfigSpec.DoubleValue GO_TO_TARGET_INTERVAL;
    public static final ForgeConfigSpec.DoubleValue PATH_CHECK_INTERVAL;
    public static final ForgeConfigSpec.DoubleValue STUCK_SECONDS_BEFORE_BREAKANDBUILD;
    public static final ForgeConfigSpec.DoubleValue DAMAGE_STORE_TIME;
    public static final ForgeConfigSpec.DoubleValue BUILT_BLOCKS_PROTECTION_TIME;

    public static final ForgeConfigSpec.ConfigValue<String> PLACE_SOUND_ID;
    public static final ForgeConfigSpec.ConfigValue<String> HIT_SOUND_ID;
    public static final ForgeConfigSpec.ConfigValue<String> BREAK_SOUND_ID;

    public static Set<ResourceLocation> DANGEROUS_BLOCKS_SET = Set.of();
    public static Set<ResourceLocation> IMPASSABLE_BLOCKS_SET = Set.of();

    // Simple resource location validator: "namespace:path[/path...]"
    private static final Pattern RL = Pattern.compile("^[a-z0-9_.-]+:[a-z0-9_/.-]+$");

    private static boolean isResLoc(Object o)
    {
        return o instanceof String s && RL.matcher(s).matches();
    }

    static
    {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        // ======================
        // [BLOCKS]
        // ======================
        b.push("blocks");

        BRIDGE_BLOCK_ID = b
                .worldRestart()
                .comment(
                        "Block ID to build bridges with. Example: 'minecraft:gravel' or 'minecraft:oak_planks'.",
                        "[Default: minecraft:dirt]"
                )
                .define("bridgeBlock", "minecraft:dirt", Config::isResLoc);

        DANGEROUS_BLOCKS = b
                .comment(
                        "Blocks that should be treated as dangerous.",
                        "Each entry must be a resource location 'modid:block'.",
                        "[Example: minecraft:campfire, minecraft:magma_block]"
                )
                .defineListAllowEmpty(
                        List.of("dangerousBlocks"),
                        List.of(
                                "minecraft:fire",
                                "minecraft:soul_fire",
                                "minecraft:campfire",
                                "minecraft:soul_campfire",
                                "minecraft:cactus",
                                "minecraft:magma_block",
                                "minecraft:sweet_berry_bush",
                                "minecraft:wither_rose",
                                "minecraft:powder_snow"
                        ),
                        Config::isResLoc
                );

        IMPASSABLE_BLOCKS = b
                .comment(
                        "Blocks that zombies should break in their path, even if they are passable by collision.",
                        "Each entry must be a resource location 'modid:block'.",
                        "[Example: minecraft:campfire, minecraft:magma_block]"
                )
                .defineListAllowEmpty(
                        List.of("impassableBlocks"),
                        List.of(
                                "minecraft:cobweb",
                                "minecraft:pointed_dripstone"
                        ),
                        Config::isResLoc
                );

        b.pop();

        // ======================
        // [AI / BEHAVIOR]
        // ======================
        b.push("behavior");

        ALWAYS_SEE_NEAREST_PLAYER = b
                .comment(
                        "Mobs always ‘see’ the nearest player at any distance.",
                        "[Default: false]"
                )
                .define("alwaysSeeNearestPlayer", false);

        APPLY_TO_ALL_HOSTILES = b
                .comment(
                        "Apply to all hostile mobs (true) or only zombies (false).",
                        "[Default: true]"
                )
                .define("applyToAllHostiles", true);

        ATTACK_ALL_ENTITIES = b
                .comment(
                        "Will the zombie attack all non-hostile creatures (true) or will it only attack its default targets (false).",
                        "[Default: false]"
                )
                .define("attackAllEntities", false);

        TARGET_SEARCH_RADIUS = b
                .comment(
                        "Target search radius (in blocks).",
                        "[Default: 35]"
                )
                .defineInRange("targetSearchRadius", 35, 4, 1024);

        DANGEROUS_BLOCKS_SCAN_RADIUS = b
                .comment(
                        "How far around the mob to scan for dangerous blocks (in blocks).",
                        "[Default: 1]"
                )
                .defineInRange("dangerousBlocksScanRadius", 1, 1, 15);

        b.pop();

        // ======================
        // [BALANCE / TIMERS]
        // ======================
        b.push("balance");

        BREAK_COOLDOWN = b
                .comment(
                        "Cooldown for breaking blocks (seconds).",
                        "[Default: 1.0]"
                )
                .defineInRange("breakCooldown", 1.0D, 0.0D, 50.0D);

        BUILD_COOLDOWN = b
                .comment(
                        "Cooldown for placing blocks (seconds).",
                        "[Default: 1.0]"
                )
                .defineInRange("buildCooldown", 1.0D, 0.0D, 50.0D);

        DAMAGE_TO_BLOCKS = b
                .comment(
                        "Damage dealt to blocks (abstract value).",
                        "[Default: 3]"
                )
                .defineInRange("damageToBlocks", 3, 1, 1000);

        FREEZE_TIME = b
                .comment(
                        "Freeze time after building or breaking a block (sec).",
                        "[Default: 0.5]"
                )
                .defineInRange("freezeTime", 0.5, 0, 5);

        SEARCH_DANGEROUS_INTERVAL = b
                .comment(
                        "Interval between dangerous blocks scans (sec).",
                        "[Default: 1]"
                )
                .defineInRange("searchDangerousInterval", 1d, 0, 10);

        GO_TO_TARGET_INTERVAL = b
                .comment(
                        "Interval between target position updates (sec).",
                        "[Default: 0.5]"
                )
                .defineInRange("goToTargetInterval", 0.5, 0, 5);

        PATH_CHECK_INTERVAL = b
                .comment(
                        "Interval between path check (sec).",
                        "[Default: 2]"
                )
                .defineInRange("pathCheckInterval", 2d, 0, 10);

        STUCK_SECONDS_BEFORE_BREAKANDBUILD = b
                .comment(
                        "How many seconds the mob must be stuck before it starts breaking and building blocks (sec).",
                        "[Default: 3]"
                )
                .defineInRange("stuckSecondsBeforeBreakAndBuild", 3d, 0, 10);

        DAMAGE_STORE_TIME = b
                .comment(
                        "How long block damage progress is stored in memory (sec).",
                        "[Default: 60]"
                )
                .defineInRange("damageStoreTime", 60d, 10, 600);

        BUILT_BLOCKS_PROTECTION_TIME = b
                .comment(
                        "How long will zombies be unable to break a block that has just been placed by other zombies? (sec).",
                        "[Default: 0.75]"
                )
                .defineInRange("builtBlocksProtectionTime", 0.75d, 0, 100);

        b.pop();

        // ======================
        // [AUDIO]
        // ======================
        b.push("audio");

        PLACE_SOUND_ID = b
                .worldRestart()
                .comment(
                        "Sound when placing a bridge block. Must be a sound event ID 'namespace:path'.",
                        "[Default: minecraft:block.rooted_dirt.place]"
                )
                .define("placeSound", "minecraft:block.rooted_dirt.place", Config::isResLoc);

        HIT_SOUND_ID = b
                .worldRestart()
                .comment(
                        "Sound when hitting a block.",
                        "[Default: minecraft:entity.zombie.attack_wooden_door]"
                )
                .define("hitSound", "minecraft:entity.zombie.attack_wooden_door", Config::isResLoc);

        BREAK_SOUND_ID = b
                .worldRestart()
                .comment(
                        "Sound when breaking a block.",
                        "[Default: minecraft:entity.zombie.break_wooden_door]"
                )
                .define("breakSound", "minecraft:entity.zombie.break_wooden_door", Config::isResLoc);

        b.pop();

        SPEC = b.build();
    }

    public static void rebuildSets()
    {
        DANGEROUS_BLOCKS_SET = parseListToSet(DANGEROUS_BLOCKS.get());
        IMPASSABLE_BLOCKS_SET = parseListToSet(IMPASSABLE_BLOCKS.get());
    }

    private static Set<ResourceLocation> parseListToSet(List<? extends String> list)
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
