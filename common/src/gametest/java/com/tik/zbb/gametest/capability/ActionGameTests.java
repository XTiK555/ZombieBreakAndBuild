package com.tik.zbb.gametest.capability;

import com.tik.zbb.gametest.fixture.GameTestConfig;
import com.tik.zbb.gametest.fixture.GameTestEntities;
import com.tik.zbb.gametest.fixture.MutableTestValue;
import com.tik.zbb.gametest.fixture.WorldGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

public final class ActionGameTests
{
    private ActionGameTests() {}

    public static void breakActionRespectsOverrideAndHardnessCap(GameTestHelper helper)
    {
        GameTestConfig.accelerateActions(helper);
        GameTestConfig.add(helper, "ai.ignoreBuildEntityIdList", "minecraft:zombie");
        GameTestConfig.set(helper, "balance.blockDamage.maximumBreakableBlockHardness", 10);
        GameTestConfig.put(helper, "balance.blockDamage.blockHealthOverrideMap", "minecraft:stone", 1);
        WorldGameTest.corridor(helper, 0, 7, 2, 1);
        WorldGameTest.corridor(helper, 0, 7, 7, 1);
        WorldGameTest.barrier(helper, 3, 2, Blocks.STONE);
        WorldGameTest.barrier(helper, 3, 7, Blocks.OBSIDIAN);
        ServerPlayer stoneTarget = GameTestEntities.serverPlayer(helper, new BlockPos(6, 2, 2), GameType.SURVIVAL);
        ServerPlayer obsidianTarget = GameTestEntities.serverPlayer(helper, new BlockPos(6, 2, 7), GameType.SURVIVAL);
        Zombie stoneZombie = GameTestEntities.zombie(helper, new BlockPos(1, 2, 2));
        Zombie obsidianZombie = GameTestEntities.zombie(helper, new BlockPos(1, 2, 7));
        GameTestEntities.keepTargeting(helper, stoneZombie, stoneTarget);
        GameTestEntities.keepTargeting(helper, obsidianZombie, obsidianTarget);

        helper.onEachTick(() -> helper.assertTrue(WorldGameTest.barrierIs(helper, 3, 7, Blocks.OBSIDIAN),
                "Block above the hardness cap was broken"));
        helper.succeedWhen(() ->
        {
            helper.assertTrue(WorldGameTest.barrierHasAir(helper, 3, 2),
                    "Stone override was not broken by real AI");
        });
    }

    public static void breakActionAccumulatesDamageBreaksAndEnforcesCooldown(GameTestHelper helper)
    {
        GameTestConfig.set(helper, "ai.alwaysSeeNearestPlayer", true);
        GameTestConfig.add(helper, "ai.ignoreBuildEntityIdList", "minecraft:zombie");
        GameTestConfig.set(helper, "balance.blockDamage.damageToBlocks", 1);
        GameTestConfig.put(helper, "balance.blockDamage.blockHealthOverrideMap", "minecraft:stone", 3);
        GameTestConfig.set(helper, "balance.cooldowns.breakCooldown", 0.5);
        WorldGameTest.corridor(helper, 0, 6, 2, 1);
        WorldGameTest.barrier(helper, 3, 2, Blocks.STONE);
        ServerPlayer player = GameTestEntities.serverPlayer(helper, new BlockPos(5, 2, 2), GameType.SURVIVAL);
        Zombie zombie = GameTestEntities.zombie(helper, new BlockPos(1, 2, 2));
        GameTestEntities.keepTargeting(helper, zombie, player);

        helper.runAtTickTime(12, () -> helper.assertTrue(WorldGameTest.barrierIs(helper, 3, 2, Blocks.STONE),
                "Stone broke before multiple real cooldown intervals elapsed"));
        helper.succeedWhen(() ->
        {
            helper.assertTrue(WorldGameTest.barrierHasAir(helper, 3, 2),
                    "Stone did not break after the required real cooldown intervals");
        });
    }

    public static void ignoreBreakMobDoesNotBreakObstacle(GameTestHelper helper)
    {
        GameTestConfig.accelerateActions(helper);
        GameTestConfig.add(helper, "ai.ignoreBuildEntityIdList", "minecraft:zombie");
        GameTestConfig.add(helper, "ai.ignoreBreakEntityIdList", "minecraft:zombie");
        WorldGameTest.corridor(helper, 0, 6, 2, 1);
        WorldGameTest.barrier(helper, 3, 2, Blocks.DIRT);
        ServerPlayer player = GameTestEntities.serverPlayer(helper, new BlockPos(5, 2, 2), GameType.SURVIVAL);
        Zombie zombie = GameTestEntities.zombie(helper, new BlockPos(1, 2, 2));
        GameTestEntities.keepTargeting(helper, zombie, player);

        helper.runAtTickTime(80, () ->
        {
            helper.assertTrue(WorldGameTest.barrierIs(helper, 3, 2, Blocks.DIRT),
                    "Zombie from ignoreBreakEntityIdList broke the obstacle");
            helper.succeed();
        });
    }

    public static void mobBuiltBlockIsTemporarilyProtected(GameTestHelper helper)
    {
        GameTestConfig.accelerateActions(helper);
        GameTestConfig.set(helper, "balance.builtBlocksProtectionTime", 1.0);
        WorldGameTest.GapLane lane = WorldGameTest.twoBlockGapLane(helper, 2, 3);
        ServerPlayer builderTarget = GameTestEntities.serverPlayer(helper, lane.target(), GameType.SURVIVAL);
        Zombie builder = GameTestEntities.zombie(helper, lane.mobStart());
        builder.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0);
        GameTestEntities.keepTargeting(helper, builder, builderTarget);
        BlockPos built = lane.firstGap();
        MutableTestValue<Zombie> breakerZombie = new MutableTestValue<>();
        MutableTestValue<ServerPlayer> breakerTarget = new MutableTestValue<>();
        MutableTestValue<Long> builtAt = new MutableTestValue<>(-1L);

        helper.onEachTick(() ->
        {
            if (breakerTarget.get() != null || WorldGameTest.state(helper, built).isAir()) return;
            builtAt.set(helper.getLevel().getGameTime());
            builder.discard();
            GameTestEntities.removeServerPlayer(helper, builderTarget);
            GameTestConfig.add(helper, "ai.ignoreBuildEntityIdList", "minecraft:zombie");
            WorldGameTest.floor(helper, 0, 1, 1, 3, 2);
            WorldGameTest.floor(helper, 3, 5, 1, 3, 2);
            for (int y = 3; y <= 5; y++)
                for (int z = 1; z <= 3; z++)
                    if (y != 3 || z != 2) helper.setBlock(new BlockPos(2, y, z), Blocks.OBSIDIAN);
            ServerPlayer target = GameTestEntities.serverPlayer(helper, new BlockPos(4, 3, 2), GameType.SURVIVAL);
            breakerTarget.set(target);
            Zombie breaker = GameTestEntities.zombie(helper, new BlockPos(1, 3, 2));
            breaker.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0);
            breaker.setTarget(target);
            breakerZombie.set(breaker);
        });
        helper.onEachTick(() ->
        {
            if (breakerZombie.get() != null && breakerTarget.get() != null)
                breakerZombie.get().setTarget(breakerTarget.get());
            if (builtAt.get() >= 0 && helper.getLevel().getGameTime() < builtAt.get() + 20)
                helper.assertFalse(WorldGameTest.state(helper, built).isAir(),
                        "A mob-built block was broken before its configured protection expired");
        });
        helper.succeedWhen(() ->
        {
            helper.assertTrue(breakerTarget.get() != null, "The mob never built the test block");
            helper.assertTrue(helper.getLevel().getGameTime() >= builtAt.get() + 20,
                    "The protection TTL did not elapse through server ticks");
            helper.assertTrue(WorldGameTest.state(helper, built).isAir(),
                    "The mob-built block stayed protected after real server ticks expired the TTL");
        });
    }

    public static void effectiveToolBreaksBlockBeforeBaselineMob(GameTestHelper helper)
    {
        GameTestConfig.set(helper, "ai.alwaysSeeNearestPlayer", true);
        GameTestConfig.add(helper, "ai.ignoreBuildEntityIdList", "minecraft:zombie");
        GameTestConfig.set(helper, "balance.blockDamage.damageToBlocks", 1);
        GameTestConfig.put(helper, "balance.blockDamage.blockHealthOverrideMap", "minecraft:stone", 20);
        GameTestConfig.set(helper, "balance.blockDamage.itemDamageMultiplierExponent", 1);
        GameTestConfig.set(helper, "balance.cooldowns.breakCooldown", 0.25);
        WorldGameTest.corridor(helper, 0, 7, 2, 1);
        WorldGameTest.corridor(helper, 0, 7, 7, 1);
        WorldGameTest.barrier(helper, 3, 2, Blocks.STONE);
        WorldGameTest.barrier(helper, 3, 7, Blocks.STONE);
        ServerPlayer baselineTarget = GameTestEntities.serverPlayer(helper, new BlockPos(6, 2, 2), GameType.SURVIVAL);
        ServerPlayer toolTarget = GameTestEntities.serverPlayer(helper, new BlockPos(6, 2, 7), GameType.SURVIVAL);
        Zombie baseline = GameTestEntities.zombie(helper, new BlockPos(1, 2, 2));
        Zombie toolUser = GameTestEntities.zombie(helper, new BlockPos(1, 2, 7));
        GameTestEntities.keepTargeting(helper, baseline, baselineTarget);
        GameTestEntities.keepTargeting(helper, toolUser, toolTarget);
        toolUser.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));

        helper.succeedWhen(() ->
        {
            helper.assertTrue(WorldGameTest.barrierHasAir(helper, 3, 7),
                    "Zombie with a pickaxe did not break its stone first");
            helper.assertTrue(WorldGameTest.barrierIs(helper, 3, 2, Blocks.STONE),
                    "Baseline zombie broke stone as early as the equipped zombie");
        });
    }

    public static void buildActionPlacesBlocksAndEnforcesCooldown(GameTestHelper helper)
    {
        GameTestConfig.set(helper, "ai.alwaysSeeNearestPlayer", true);
        GameTestConfig.set(helper, "balance.cooldowns.buildCooldown", 0.5);
        GameTestConfig.set(helper, "ai.tactics.adjustHeightToTarget", false);
        GameTestConfig.set(helper, "ai.tactics.clearObstaclesToTarget", false);
        GameTestConfig.set(helper, "ai.tactics.mitigateDangerousBlocks", false);
        WorldGameTest.GapLane lane = WorldGameTest.twoBlockGapLane(helper, 2, 3);
        ServerPlayer player = GameTestEntities.serverPlayer(helper, lane.target(), GameType.SURVIVAL);
        Zombie zombie = GameTestEntities.zombie(helper, lane.mobStart());
        GameTestEntities.keepTargeting(helper, zombie, player);
        double movementSpeed = zombie.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue();
        zombie.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0);
        BlockPos first = lane.firstGap();
        BlockPos second = lane.secondGap();
        MutableTestValue<BlockPos> firstPlaced = new MutableTestValue<>();
        MutableTestValue<Long> firstPlacedAt = new MutableTestValue<>(-1L);

        helper.onEachTick(() ->
        {
            boolean firstBuilt = !WorldGameTest.state(helper, first).isAir();
            boolean secondBuilt = !WorldGameTest.state(helper, second).isAir();
            if (firstPlaced.get() == null)
            {
                if (!firstBuilt && !secondBuilt) return;
                helper.assertFalse(firstBuilt && secondBuilt,
                        "Both bridge blocks were placed in the same tick");
                firstPlaced.set(firstBuilt ? first : second);
                firstPlacedAt.set(helper.getLevel().getGameTime());
                return;
            }

            BlockPos remaining = firstPlaced.get().equals(first) ? second : first;
            if (helper.getLevel().getGameTime() < firstPlacedAt.get() + 10)
                helper.assertTrue(WorldGameTest.state(helper, remaining).isAir(), "Second bridge block ignored build cooldown");
            else
                zombie.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(movementSpeed);
        });
        helper.succeedWhen(() ->
        {
            helper.assertTrue(firstPlaced.get() != null
                            && helper.getLevel().getGameTime() >= firstPlacedAt.get() + 10,
                    "Build cooldown did not elapse between bridge blocks");
            helper.assertTrue(WorldGameTest.state(helper, first).is(Blocks.DIRT) && WorldGameTest.state(helper, second).is(Blocks.DIRT),
                    "Zombie did not place the configured bridge blocks through real AI");
        });
    }

    public static void buildActionRejectsNonReplaceable(GameTestHelper helper)
    {
        GameTestConfig.accelerateActions(helper);
        GameTestConfig.add(helper, "ai.ignoreBreakEntityIdList", "minecraft:zombie");
        WorldGameTest.GapLane lane = WorldGameTest.twoBlockGapLane(helper, 2, 3);
        helper.setBlock(lane.firstGap(), Blocks.GOLD_BLOCK);
        ServerPlayer player = GameTestEntities.serverPlayer(helper, lane.target(), GameType.SURVIVAL);
        Zombie zombie = GameTestEntities.zombie(helper, lane.mobStart());
        GameTestEntities.keepTargeting(helper, zombie, player);

        helper.runAtTickTime(80, () ->
        {
            helper.assertTrue(WorldGameTest.state(helper, lane.firstGap()).is(Blocks.GOLD_BLOCK),
                    "Build behavior overwrote a non-replaceable block");
            helper.succeed();
        });
    }

    public static void ignoreBuildMobDoesNotBuildBridge(GameTestHelper helper)
    {
        GameTestConfig.accelerateActions(helper);
        GameTestConfig.add(helper, "ai.ignoreBuildEntityIdList", "minecraft:zombie");
        WorldGameTest.GapLane lane = WorldGameTest.twoBlockGapLane(helper, 2, 3);
        ServerPlayer player = GameTestEntities.serverPlayer(helper, lane.target(), GameType.SURVIVAL);
        Zombie zombie = GameTestEntities.zombie(helper, lane.mobStart());
        GameTestEntities.keepTargeting(helper, zombie, player);

        helper.runAtTickTime(80, () ->
        {
            helper.assertTrue(WorldGameTest.state(helper, lane.firstGap()).isAir()
                            && WorldGameTest.state(helper, lane.secondGap()).isAir(),
                    "Zombie from ignoreBuildEntityIdList built a bridge");
            helper.succeed();
        });
    }

    public static void mobUsesConfiguredBuildBlock(GameTestHelper helper)
    {
        GameTestConfig.accelerateActions(helper);
        GameTestConfig.add(helper, "ai.ignoreBreakEntityIdList", "minecraft:zombie");
        GameTestConfig.set(helper, "ai.tactics.adjustHeightToTarget", false);
        GameTestConfig.set(helper, "ai.tactics.clearObstaclesToTarget", false);
        GameTestConfig.set(helper, "ai.tactics.mitigateDangerousBlocks", false);
        GameTestConfig.put(helper, "blocks.mobPlaceBlockIdOverrideMap", "minecraft:zombie", "minecraft:gold_block");
        WorldGameTest.GapLane lane = WorldGameTest.twoBlockGapLane(helper, 2, 3);
        ServerPlayer player = GameTestEntities.serverPlayer(helper, lane.target(), GameType.SURVIVAL);
        Zombie zombie = GameTestEntities.zombie(helper, lane.mobStart());
        GameTestEntities.keepTargeting(helper, zombie, player);
        zombie.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0);
        BlockPos first = lane.firstGap();
        BlockPos second = lane.secondGap();

        helper.succeedWhen(() ->
        {
            boolean built = !WorldGameTest.state(helper, first).isAir() || !WorldGameTest.state(helper, second).isAir();
            helper.assertTrue(built, "Zombie did not build through its normal AI");
            helper.assertTrue(WorldGameTest.state(helper, first).isAir() || WorldGameTest.state(helper, first).is(Blocks.GOLD_BLOCK),
                    "Zombie ignored its configured build block");
            helper.assertTrue(WorldGameTest.state(helper, second).isAir() || WorldGameTest.state(helper, second).is(Blocks.GOLD_BLOCK),
                    "Zombie ignored its configured build block");
        });
    }

    public static void buildBlockConfigHotReloadAffectsExistingMob(GameTestHelper helper)
    {
        GameTestConfig.accelerateActions(helper);
        GameTestConfig.set(helper, "balance.cooldowns.buildCooldown", 0.5);
        GameTestConfig.add(helper, "ai.ignoreBreakEntityIdList", "minecraft:zombie");
        GameTestConfig.set(helper, "ai.tactics.adjustHeightToTarget", false);
        GameTestConfig.set(helper, "ai.tactics.clearObstaclesToTarget", false);
        GameTestConfig.set(helper, "ai.tactics.mitigateDangerousBlocks", false);
        GameTestConfig.put(helper, "blocks.mobPlaceBlockIdOverrideMap", "minecraft:zombie", "minecraft:dirt");
        WorldGameTest.GapLane lane = WorldGameTest.twoBlockGapLane(helper, 2, 3);
        ServerPlayer player = GameTestEntities.serverPlayer(helper, lane.target(), GameType.SURVIVAL);
        Zombie zombie = GameTestEntities.zombie(helper, lane.mobStart());
        GameTestEntities.keepTargeting(helper, zombie, player);
        double movementSpeed = zombie.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue();
        zombie.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0);
        BlockPos first = lane.firstGap();
        BlockPos second = lane.secondGap();
        MutableTestValue<BlockPos> firstBuilt = new MutableTestValue<>();
        MutableTestValue<Long> firstBuiltAt = new MutableTestValue<>(-1L);

        helper.onEachTick(() ->
        {
            if (firstBuilt.get() != null)
            {
                if (helper.getLevel().getGameTime() >= firstBuiltAt.get() + 10)
                    zombie.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(movementSpeed);
                return;
            }
            boolean builtFirst = !WorldGameTest.state(helper, first).isAir();
            boolean builtSecond = !WorldGameTest.state(helper, second).isAir();
            if (!builtFirst && !builtSecond) return;

            helper.assertFalse(builtFirst && builtSecond,
                    "Zombie built both blocks before the runtime config update");
            BlockPos built = builtFirst ? first : second;
            helper.assertTrue(WorldGameTest.state(helper, built).is(Blocks.DIRT),
                    "The existing zombie did not use dirt before the runtime config update");
            firstBuilt.set(built);
            firstBuiltAt.set(helper.getLevel().getGameTime());
            GameTestConfig.put(helper, "blocks.mobPlaceBlockIdOverrideMap", "minecraft:zombie", "minecraft:cobblestone");
        });
        helper.succeedWhen(() ->
        {
            helper.assertTrue(firstBuilt.get() != null, "Zombie did not perform its first build action");
            BlockPos remaining = firstBuilt.get().equals(first) ? second : first;
            helper.assertTrue(zombie.isAlive(), "The original zombie was replaced or died");
            helper.assertTrue(WorldGameTest.state(helper, firstBuilt.get()).is(Blocks.DIRT),
                    "The first block changed after the runtime config update");
            helper.assertTrue(WorldGameTest.state(helper, remaining).is(Blocks.COBBLESTONE),
                    "The same zombie did not use the updated build block through normal AI");
        });
    }
}
