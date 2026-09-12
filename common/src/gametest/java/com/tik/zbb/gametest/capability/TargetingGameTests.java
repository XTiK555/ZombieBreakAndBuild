package com.tik.zbb.gametest.capability;

import com.tik.zbb.gametest.fixture.GameTestConfig;
import com.tik.zbb.gametest.fixture.GameTestEntities;
import com.tik.zbb.gametest.fixture.MutableTestValue;
import com.tik.zbb.gametest.fixture.WorldGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

public final class TargetingGameTests
{
    private TargetingGameTests() {}

    public static void nearestTargetThroughWallRespectsSolidBlockLimit(GameTestHelper helper)
    {
        int limit = 3;
        int followRange = 7;

        GameTestConfig.set(helper, "ai.canNoticeTargetsThroughBlocks", true);
        GameTestConfig.set(helper, "ai.noticeTargetsThroughBlocksLimit", limit);
        GameTestConfig.set(helper, "ai.alwaysSeeNearestPlayer", false);
        GameTestConfig.clear(helper, "ai.affectedEntityIdList");
        GameTestConfig.add(helper, "ai.affectedEntityIdList", "minecraft:zombie");
        GameTestConfig.add(helper, "ai.ignoreBreakEntityIdList", "minecraft:zombie");
        GameTestConfig.add(helper, "ai.ignoreBuildEntityIdList", "minecraft:zombie");

        WorldGameTest.floor(helper, 0, 9, 0, 10, 1);
        for (int y = 2; y <= 5; y++)
            for (int z = 1; z <= 3; z++)
                WorldGameTest.solidRay(helper, 2, limit, y, z);
        for (int y = 2; y <= 5; y++)
            for (int z = 7; z <= 9; z++)
                WorldGameTest.solidRay(helper, 2, limit + 1, y, z);

        Zombie acceptedZombie = GameTestEntities.shortSightedStationaryZombie(helper, new BlockPos(1, 2, 2), followRange);
        acceptedZombie.setBaby(false);
        var acceptedTarget = GameTestEntities.vulnerableFrozenVillager(helper, new BlockPos(6, 2, 2));
        MutableTestValue<Zombie> rejectedZombie = new MutableTestValue<>();
        MutableTestValue<Long> rejectedSince = new MutableTestValue<>(-1L);

        helper.onEachTick(() ->
        {
            if (rejectedZombie.get() == null)
            {
                if (acceptedZombie.getTarget() != null && acceptedZombie.getTarget() != acceptedTarget)
                    helper.fail("Accepted zombie acquired unexpected target: " + acceptedZombie.getTarget().getType() + " at " + acceptedZombie.getTarget().blockPosition());
                if (acceptedZombie.getTarget() != acceptedTarget) return;
                acceptedZombie.discard();
                acceptedTarget.discard();
                rejectedZombie.set(GameTestEntities.shortSightedStationaryZombie(helper, new BlockPos(1, 2, 8), followRange));
                rejectedZombie.get().setBaby(false);
                GameTestEntities.vulnerableFrozenVillager(helper, new BlockPos(7, 2, 8));
                rejectedSince.set(helper.getLevel().getGameTime());
                return;
            }

            helper.assertTrue(rejectedZombie.get().getTarget() == null,
                    "Zombie acquired a target behind N + 1 solid blocks");
            if (helper.getLevel().getGameTime() >= rejectedSince.get() + 80)
                helper.succeed();
        });
    }

    public static void continueTargetThroughWallRespectsSolidBlockLimit(GameTestHelper helper)
    {
        int limit = 3;

        GameTestConfig.set(helper, "ai.canContinueSeeingTargetsThroughBlocks", true);
        GameTestConfig.set(helper, "ai.continueSeeingTargetsThroughBlocksLimit", limit);
        GameTestConfig.set(helper, "ai.canNoticeTargetsThroughBlocks", false);
        GameTestConfig.set(helper, "ai.alwaysSeeNearestPlayer", false);
        GameTestConfig.add(helper, "ai.ignoreBreakEntityIdList", "minecraft:zombie");
        GameTestConfig.add(helper, "ai.ignoreBuildEntityIdList", "minecraft:zombie");
        GameTestConfig.clear(helper, "ai.affectedEntityIdList");
        GameTestConfig.add(helper, "ai.affectedEntityIdList", "minecraft:zombie");
        WorldGameTest.floor(helper, 0, 9, 0, 4, 1);
        WorldGameTest.corridorWalls(helper, 0, 9, 2, 1);

        Zombie zombie = GameTestEntities.stationaryZombie(helper, new BlockPos(1, 2, 2));
        ServerPlayer target = GameTestEntities.serverPlayer(helper, new BlockPos(8, 2, 2), GameType.SURVIVAL);
        MutableTestValue<Integer> phase = new MutableTestValue<>(0);
        MutableTestValue<Long> deadline = new MutableTestValue<>(-1L);
        long wallsAt = helper.getLevel().getGameTime() + 20;

        helper.onEachTick(() ->
        {
            long gameTime = helper.getLevel().getGameTime();
            if (phase.get() == 0)
            {
                if (gameTime < wallsAt || zombie.getTarget() != target) return;
                for (int y = 2; y <= 5; y++)
                    for (int z = 1; z <= 3; z++) WorldGameTest.solidRay(helper, 2, limit, y, z);
                deadline.set(gameTime + 80);
                phase.set(1);
                return;
            }

            if (phase.get() == 1)
            {
                helper.assertTrue(zombie.getTarget() == target,
                        "Zombie lost a target behind exactly N solid blocks");
                if (gameTime < deadline.get()) return;

                for (int y = 2; y <= 5; y++)
                    for (int z = 1; z <= 3; z++) helper.setBlock(new BlockPos(2 + limit, y, z), Blocks.STONE);
                deadline.set(gameTime + 160);
                phase.set(2);
                return;
            }

            helper.assertTrue(zombie.getTarget() == target || zombie.getTarget() == null,
                    "Zombie switched to an unexpected target after the wall grew past the limit");
            if (zombie.getTarget() == null)
            {
                helper.succeed();
                return;
            }
            helper.assertTrue(gameTime <= deadline.get(),
                    "Zombie retained a target behind N + 1 solid blocks");
        });
    }

    public static void throughWallTargetingDoesNotApplyToUnaffectedMob(GameTestHelper helper)
    {
        GameTestConfig.set(helper, "ai.canNoticeTargetsThroughBlocks", true);
        GameTestConfig.add(helper, "ai.affectedEntityIdList", "!minecraft:zombie");
        GameTestConfig.add(helper, "ai.ignoreBreakEntityIdList", "minecraft:zombie");
        WorldGameTest.corridor(helper, 0, 7, 2, 1);
        WorldGameTest.barrier(helper, 3, 2, Blocks.OBSIDIAN);
        Zombie zombie = GameTestEntities.stationaryZombie(helper, new BlockPos(1, 2, 2));
        var target = GameTestEntities.vulnerableFrozenVillager(helper, new BlockPos(6, 2, 2));
        helper.assertFalse(zombie.getSensing().hasLineOfSight(target),
                "Test wall did not block vanilla line of sight");

        helper.runAtTickTime(60, () ->
        {
            helper.assertTrue(zombie.getTarget() == null,
                    "A zombie excluded by affectedEntityIdList gained through-wall targeting");
            helper.succeed();
        });
    }

    public static void throughWallTargetingDisabledUsesVanillaVisibility(GameTestHelper helper)
    {
        class TestState
        {
            int phase = 0;
            long deadline;
            Zombie visibleZombie;
            ServerPlayer visibleTarget;
        }

        int hiddenObserveTicks = 60;
        int acquireTimeoutTicks = 120;
        int loseTimeoutTicks = 300;

        GameTestConfig.set(helper, "ai.canNoticeTargetsThroughBlocks", false);
        GameTestConfig.set(helper, "ai.canContinueSeeingTargetsThroughBlocks", false);
        GameTestConfig.set(helper, "ai.alwaysSeeNearestPlayer", false);
        GameTestConfig.add(helper, "ai.ignoreBreakEntityIdList", "minecraft:zombie");
        GameTestConfig.add(helper, "ai.ignoreBuildEntityIdList", "minecraft:zombie");
        WorldGameTest.floor(helper, 0, 7, 1, 3, 1);
        WorldGameTest.barrier(helper, 3, 2, Blocks.OBSIDIAN);

        Zombie hiddenZombie = GameTestEntities.stationaryZombie(helper, new BlockPos(1, 2, 2));
        var hiddenTarget = GameTestEntities.vulnerableFrozenVillager(helper, new BlockPos(6, 2, 2));
        TestState state = new TestState();
        state.deadline = helper.getLevel().getGameTime() + hiddenObserveTicks;

        helper.onEachTick(() ->
        {
            long gameTime = helper.getLevel().getGameTime();

            switch (state.phase)
            {
                case 0 ->
                {
                    helper.assertTrue(
                            hiddenZombie.getTarget() == null,
                            "Zombie acquired a target through a wall while through-wall notice was disabled"
                    );

                    if (gameTime < state.deadline) return;

                    hiddenZombie.discard();
                    hiddenTarget.discard();
                    for (int y = 2; y <= 4; y++)
                    {
                        for (int z = 1; z <= 3; z++)
                        {
                            helper.setBlock(new BlockPos(3, y, z), Blocks.AIR);
                        }
                    }

                    state.visibleZombie = GameTestEntities.stationaryZombie(helper, new BlockPos(1, 2, 2));
                    state.visibleTarget = GameTestEntities.serverPlayer(helper, new BlockPos(6, 2, 2), GameType.SURVIVAL);
                    state.deadline = gameTime + acquireTimeoutTicks;
                    state.phase = 1;
                }
                case 1 ->
                {
                    if (state.visibleZombie.getTarget() == state.visibleTarget)
                    {
                        WorldGameTest.barrier(helper, 3, 2, Blocks.OBSIDIAN);

                        state.deadline = gameTime + loseTimeoutTicks;
                        state.phase = 2;
                        return;
                    }
                    helper.assertTrue(
                            gameTime <= state.deadline,
                            "Zombie did not acquire its visible target through vanilla AI"
                    );
                }
                case 2 ->
                {
                    if (state.visibleZombie.getTarget() == null)
                    {
                        state.phase = 3;
                        helper.succeed();
                        return;
                    }

                    helper.assertTrue(
                            state.visibleZombie.getTarget() == state.visibleTarget,
                            "Zombie switched to an unexpected target after the wall appeared"
                    );
                    helper.assertTrue(
                            gameTime <= state.deadline,
                            "Zombie retained a hidden target while through-wall continuation was disabled"
                    );
                }
            }
        });
    }

    public static void alwaysSeeNearestPlayerRespectsToggleAndChoosesNearestValidPlayer(GameTestHelper helper)
    {
        GameTestConfig.set(helper, "ai.alwaysSeeNearestPlayer", false);
        WorldGameTest.floor(helper, 0, 10, 1, 3, 1);
        WorldGameTest.barrier(helper, 3, 2, Blocks.OBSIDIAN);
        Zombie zombie = GameTestEntities.stationaryZombie(helper, new BlockPos(1, 2, 2));
        ServerPlayer creative = GameTestEntities.serverPlayer(helper, new BlockPos(4, 2, 2), GameType.CREATIVE);
        ServerPlayer spectator = GameTestEntities.serverPlayer(helper, new BlockPos(5, 2, 2), GameType.SPECTATOR);
        ServerPlayer nearest = GameTestEntities.serverPlayer(helper, new BlockPos(7, 2, 2), GameType.SURVIVAL);
        ServerPlayer farther = GameTestEntities.serverPlayer(helper, new BlockPos(9, 2, 2), GameType.SURVIVAL);

        helper.runAtTickTime(30, () ->
        {
            helper.assertTrue(zombie.getTarget() == null, "Disabled option still acquired a player");
            GameTestConfig.set(helper, "ai.alwaysSeeNearestPlayer", true);
        });
        helper.succeedWhen(() ->
        {
            helper.assertTrue(zombie.getTarget() == nearest,
                    "Zombie did not choose the nearest survival player after the command");
        });
    }

    public static void alwaysSeeNearestPlayerDoesNotAggroNonAngryNeutralMob(GameTestHelper helper)
    {
        GameTestConfig.set(helper, "ai.alwaysSeeNearestPlayer", true);
        WorldGameTest.floor(helper, 0, 6, 1, 3, 1);
        var piglin = helper.spawn(EntityTypes.ZOMBIFIED_PIGLIN, new BlockPos(2, 2, 2), EntitySpawnReason.COMMAND);
        piglin.setInvulnerable(true);
        piglin.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0);
        ServerPlayer player = GameTestEntities.serverPlayer(helper, new BlockPos(5, 2, 2), GameType.SURVIVAL);

        helper.runAtTickTime(30, () ->
        {
            helper.assertTrue(piglin.getTarget() == null, "Non-angry zombified piglin acquired a player");
            piglin.setPersistentAngerTarget(EntityReference.of(player));
            piglin.setTimeToRemainAngry(600);
        });
        helper.succeedWhen(() ->
        {
            helper.assertTrue(piglin.getTarget() == player,
                    "Angry zombified piglin did not acquire its anger target through normal AI");
        });
    }

    public static void mobFilterConfigChangesTargetingBehavior(GameTestHelper helper)
    {
        GameTestConfig.set(helper, "ai.alwaysSeeNearestPlayer", true);
        GameTestConfig.add(helper, "ai.affectedEntityIdList", "!minecraft:zombie");
        WorldGameTest.floor(helper, 0, 7, 1, 3, 1);
        WorldGameTest.barrier(helper, 3, 2, Blocks.OBSIDIAN);
        Zombie zombie = GameTestEntities.stationaryZombie(helper, new BlockPos(1, 2, 2));
        ServerPlayer player = GameTestEntities.serverPlayer(helper, new BlockPos(6, 2, 2), GameType.SURVIVAL);

        helper.runAtTickTime(35, () ->
        {
            helper.assertTrue(zombie.getTarget() == null, "Excluded zombie acquired ZBB target behavior");
            GameTestConfig.reset(helper, "ai.affectedEntityIdList");
        });
        helper.succeedWhen(() ->
        {
            helper.assertTrue(zombie.getTarget() == player,
                    "The same zombie did not acquire the player after its filter changed");
        });
    }

}
