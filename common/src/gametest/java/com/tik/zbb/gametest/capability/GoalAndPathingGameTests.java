package com.tik.zbb.gametest.capability;

import com.tik.zbb.gametest.fixture.GameTestConfig;
import com.tik.zbb.gametest.fixture.GameTestEntities;
import com.tik.zbb.gametest.fixture.MutableTestValue;

import com.tik.zbb.gametest.fixture.WorldGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

public final class GoalAndPathingGameTests
{
    private GoalAndPathingGameTests() {}

    public static void mobBreaksWallToReachTarget(GameTestHelper helper)
    {
        GameTestConfig.accelerateActions(helper);
        GameTestConfig.add(helper, "ai.ignoreBuildEntityIdList", "minecraft:zombie");
        GameTestConfig.put(helper, "balance.blockDamage.blockHealthOverrideMap", "minecraft:dirt", 1);
        WorldGameTest.corridor(helper, 0, 7, 2, 1);
        WorldGameTest.barrier(helper, 3, 2, Blocks.DIRT);
        ServerPlayer player = GameTestEntities.serverPlayer(helper, new BlockPos(6, 2, 2), GameType.SURVIVAL);
        Zombie zombie = GameTestEntities.zombie(helper, new BlockPos(1, 2, 2));
        GameTestEntities.keepTargeting(helper, zombie, player);

        helper.succeedWhen(() ->
        {
            helper.assertTrue(WorldGameTest.barrierHasAir(helper, 3, 2),
                    "Zombie did not break a passage through the wall");
            helper.assertTrue(zombie.distanceToSqr(player) < 4.0,
                    "Zombie did not pass through the wall and reach its target");
        });
    }

    public static void mobBridgesGapToReachTarget(GameTestHelper helper)
    {
        GameTestConfig.accelerateActions(helper);
        GameTestConfig.add(helper, "ai.ignoreBreakEntityIdList", "minecraft:zombie");
        GameTestConfig.set(helper, "ai.tactics.adjustHeightToTarget", false);
        GameTestConfig.set(helper, "ai.tactics.clearObstaclesToTarget", false);
        GameTestConfig.set(helper, "ai.tactics.mitigateDangerousBlocks", false);
        WorldGameTest.GapLane lane = WorldGameTest.twoBlockGapLane(helper, 2, 3);
        ServerPlayer player = GameTestEntities.serverPlayer(helper, lane.target(), GameType.SURVIVAL);
        Zombie zombie = GameTestEntities.zombie(helper, lane.mobStart());
        GameTestEntities.keepTargeting(helper, zombie, player);
        double movementSpeed = zombie.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue();
        zombie.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0);
        BlockPos firstGap = lane.firstGap();
        BlockPos secondGap = lane.secondGap();
        MutableTestValue<Boolean> resumed = new MutableTestValue<>(false);

        helper.onEachTick(() ->
        {
            boolean placedInGap = !WorldGameTest.state(helper, firstGap).isAir() || !WorldGameTest.state(helper, secondGap).isAir();
            if (placedInGap && !resumed.get())
            {
                resumed.set(true);
                zombie.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(movementSpeed);
            }
        });

        helper.succeedWhen(() ->
        {
            helper.assertTrue(
                    !WorldGameTest.state(helper, firstGap).isAir()
                            && !WorldGameTest.state(helper, secondGap).isAir(),
                    "Zombie did not complete the bridge"
            );
            helper.assertTrue(
                    zombie.distanceToSqr(player) < 4.0,
                    "Zombie did not cross the gap and reach its target"
            );
        });
    }

    public static void mobClimbsTowardHigherTarget(GameTestHelper helper)
    {
        GameTestConfig.accelerateActions(helper);
        GameTestConfig.add(helper, "ai.ignoreBreakEntityIdList", "minecraft:zombie");
        GameTestConfig.set(helper, "ai.tactics.bridgeToTarget", false);
        WorldGameTest.floor(helper, 1, 3, 1, 3, 1);
        ServerPlayer player = GameTestEntities.serverPlayer(helper, new BlockPos(2, 6, 2), GameType.SURVIVAL);
        player.setNoGravity(true);
        Zombie zombie = GameTestEntities.zombie(helper, new BlockPos(2, 2, 2));
        zombie.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0);
        GameTestEntities.keepTargeting(helper, zombie, player);
        double startY = zombie.getY();

        helper.succeedWhen(() ->
        {
            helper.assertTrue(zombie.getY() >= startY + 1,
                    "Zombie did not climb after placing support");
            helper.assertTrue(!zombie.level().getBlockState(zombie.blockPosition().below()).isAir(),
                    "Zombie did not place support while pursuing a higher target");
        });
    }

    public static void mitigateDangerousBlocksCoversOrBreaksDanger(GameTestHelper helper)
    {
        GameTestConfig.accelerateActions(helper);
        WorldGameTest.floor(helper, 0, 6, 0, 6, 1);
        helper.setBlock(new BlockPos(2, 2, 1), Blocks.FIRE);
        helper.setBlock(new BlockPos(2, 1, 5), Blocks.MAGMA_BLOCK);
        ServerPlayer fireTarget = GameTestEntities.serverPlayer(helper, new BlockPos(5, 2, 1), GameType.SURVIVAL);
        ServerPlayer magmaTarget = GameTestEntities.serverPlayer(helper, new BlockPos(5, 2, 5), GameType.SURVIVAL);
        Zombie fireZombie = GameTestEntities.zombie(helper, new BlockPos(1, 2, 1));
        Zombie magmaZombie = GameTestEntities.zombie(helper, new BlockPos(1, 2, 5));
        GameTestEntities.keepTargeting(helper, fireZombie, fireTarget);
        GameTestEntities.keepTargeting(helper, magmaZombie, magmaTarget);

        helper.succeedWhen(() ->
        {
            helper.assertTrue(!WorldGameTest.state(helper, new BlockPos(2, 2, 1)).is(Blocks.FIRE)
                            && !WorldGameTest.state(helper, new BlockPos(2, 2, 1)).isAir(),
                    "Zombie did not cover replaceable fire");
            helper.assertTrue(WorldGameTest.state(helper, new BlockPos(2, 1, 5)).isAir(),
                    "Zombie did not break solid magma in its path");
        });
    }

    public static void reachablePathDoesNotModifyWorld(GameTestHelper helper)
    {
        WorldGameTest.floor(helper, 0, 6, 0, 2, 1);
        WorldGameTest.floor(helper, 0, 6, 9, 11, 1);
        WorldGameTest.floor(helper, 3, 6, 9, 11, 2);
        WorldGameTest.corridorWalls(helper, 0, 6, 1, 1);
        WorldGameTest.corridorWalls(helper, 0, 6, 10, 1);

        Zombie flat = GameTestEntities.zombie(helper, new BlockPos(1, 2, 1));
        Zombie higher = GameTestEntities.zombie(helper, new BlockPos(1, 2, 10));
        var flatTarget = GameTestEntities.frozenVillager(helper, new BlockPos(5, 2, 1));
        var higherTarget = GameTestEntities.frozenVillager(helper, new BlockPos(5, 3, 10));
        GameTestEntities.keepTargeting(helper, flat, flatTarget);
        GameTestEntities.keepTargeting(helper, higher, higherTarget);
        double flatStart = flat.distanceToSqr(flatTarget);
        double higherStart = higher.distanceToSqr(higherTarget);
        Map<BlockPos, BlockState> before = WorldGameTest.snapshot(helper, 0, 10, 1, 6, 0, 11);

        helper.onEachTick(() -> WorldGameTest.assertUnchanged(helper, before));
        helper.succeedWhen(() ->
        {
            helper.assertTrue(
                    flat.distanceToSqr(flatTarget) < flatStart - 1,
                    "Zombie did not follow a flat reachable path"
            );
            helper.assertTrue(
                    higher.distanceToSqr(higherTarget) < higherStart - 1,
                    "Zombie did not follow a reachable path to a higher target"
            );
            WorldGameTest.assertUnchanged(helper, before);
        });
    }

    public static void mobWithoutTargetDoesNotModifyWorld(GameTestHelper helper)
    {
        GameTestConfig.set(helper, "balance.cooldowns.searchDangerousBlocksCooldown", 0);
        WorldGameTest.floor(helper, 0, 6, 0, 4, 1);
        WorldGameTest.barrier(helper, 4, 2, Blocks.DIRT);
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.MAGMA_BLOCK);
        Zombie zombie = GameTestEntities.zombie(helper, new BlockPos(1, 2, 2));
        Map<BlockPos, BlockState> before = WorldGameTest.snapshot(helper, 0, 6, 1, 4, 0, 4);

        helper.onEachTick(() ->
        {
            helper.assertTrue(
                    zombie.getTarget() == null,
                    "No-target test zombie unexpectedly acquired a target"
            );

            WorldGameTest.assertUnchanged(helper, before);
        });
        helper.runAtTickTime(120, () ->
                helper.succeed()
        );
    }

}
