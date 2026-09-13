package com.tik.zbb.gametest.capability;

import com.tik.zbb.gametest.fixture.*;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class BlockStorageGameTests
{
    private BlockStorageGameTests() {}

    public static void builtBlockDisappearsAndRestoresPreviousState(GameTestHelper helper)
    {
        configureBuilding(helper);
        GameTestConfig.set(helper, "blockRestoration.builtBlocksDisappearing", true);
        GameTestConfig.set(helper, "blockRestoration.builtBlocksDisappearTime", 0.5);
        WorldGameTest.GapLane lane = WorldGameTest.twoBlockGapLane(helper, 2, 3);
        BlockPos position = lane.firstGap();
        helper.setBlock(position, Blocks.WATER);
        MutableTestValue<Boolean> built = new MutableTestValue<>(false);

        DirectMobActions.forMob(helper, GameTestEntities.frozenZombie(helper, lane.mobStart())).build(position);
        helper.onEachTick(() ->
        {
            if (WorldGameTest.state(helper, position).is(Blocks.DIRT)) built.set(true);
        });
        helper.succeedWhen(() ->
        {
            helper.assertTrue(built.get(), "Zombie never built into the replaceable position");
            helper.assertTrue(WorldGameTest.state(helper, position).is(Blocks.WATER),
                    "Disappearing mob-built block did not restore the previous water state");
        });
    }

    public static void externalReplacementCancelsBuiltBlockRestorationWithoutOverwrite(GameTestHelper helper)
    {
        configureBuilding(helper);
        GameTestConfig.set(helper, "blockRestoration.builtBlocksDisappearing", true);
        GameTestConfig.set(helper, "blockRestoration.builtBlocksDisappearTime", 0.5);
        WorldGameTest.GapLane lane = WorldGameTest.twoBlockGapLane(helper, 2, 3);
        BlockPos built = lane.firstGap();
        MutableTestValue<Long> replacedAt = new MutableTestValue<>(-1L);

        DirectMobActions.forMob(helper, GameTestEntities.frozenZombie(helper, lane.mobStart())).build(built);
        helper.onEachTick(() ->
        {
            if (replacedAt.get() >= 0 || !WorldGameTest.state(helper, built).is(Blocks.DIRT)) return;
            helper.setBlock(built, Blocks.OBSIDIAN);
            replacedAt.set(helper.getLevel().getGameTime());
        });
        helper.succeedWhen(() ->
        {
            helper.assertTrue(replacedAt.get() >= 0
                            && helper.getLevel().getGameTime() >= replacedAt.get() + 15,
                    "The zombie never built the tracked block or its TTL did not elapse");
            helper.assertTrue(WorldGameTest.state(helper, built).is(Blocks.OBSIDIAN),
                    "Built-block restoration overwrote an external Minecraft replacement");
        });
    }

    public static void builtBlockRestoresAfterChunkUnloadWithoutStaleVisual(GameTestHelper helper)
    {
        configureBuilding(helper);
        GameTestConfig.set(helper, "blockRestoration.builtBlocksDisappearing", true);
        GameTestConfig.set(helper, "blockRestoration.builtBlocksDisappearTime", 2.0);
        GameTestConfig.set(helper, "visualEffects.builtDisappearBlockDisplay", true);

        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        ChunkPos chunk = new ChunkPos((origin.getX() >> 4) + 12, (origin.getZ() >> 4) + 12);
        BlockPos absolute = new BlockPos(chunk.getMiddleBlockX(), origin.getY() + 2, chunk.getMiddleBlockZ());
        BlockPos relative = absolute.subtract(origin);
        level.setChunkForced(chunk.x(), chunk.z(), true);
        level.getChunk(chunk.x(), chunk.z());
        BlockState original = level.getBlockState(absolute);
        level.setBlockAndUpdate(absolute, Blocks.WATER.defaultBlockState());

        Zombie zombie = GameTestEntities.frozenZombie(helper, new BlockPos(1, 2, 1));
        DirectMobActions actions = DirectMobActions.forMob(helper, zombie);
        actions.build(relative);

        class TestState
        {
            int phase;
            long deadline = level.getGameTime() + 100;
            long firstBuiltAt = level.getGameTime();
            long visualUnloadedAt;
            boolean chunkUnloaded;

            boolean loaded()
            {
                return level.getChunkSource().getChunkNow(chunk.x(), chunk.z()) != null;
            }

            boolean hasVisual()
            {
                for (Entity entity : level.getAllEntities())
                {
                    if (entity instanceof Display.BlockDisplay
                            && entity.entityTags().contains("zbb_build_disappear")
                            && entity.blockPosition().distManhattan(absolute) <= 1)
                        return true;
                }
                return false;
            }

            void forceLoad()
            {
                level.setChunkForced(chunk.x(), chunk.z(), true);
                level.getChunk(chunk.x(), chunk.z());
            }

            void cleanup()
            {
                forceLoad();
                level.setBlockAndUpdate(absolute, original);
                level.setChunkForced(chunk.x(), chunk.z(), false);
            }

            void fail(String message)
            {
                cleanup();
                helper.fail(message);
            }
        }

        TestState state = new TestState();
        level.setChunkForced(chunk.x(), chunk.z(), false);

        helper.onEachTick(() ->
        {
            long now = level.getGameTime();
            switch (state.phase)
            {
                case 0 ->
                {
                    if (!state.loaded())
                    {
                        state.phase = 1;
                        state.deadline = state.firstBuiltAt + 80;
                    }
                    else if (now > state.deadline)
                        state.fail("Remote chunk did not unload after its forced ticket was removed");
                }
                case 1 ->
                {
                    if (now < state.deadline) return;
                    state.forceLoad();
                    state.phase = 2;
                    state.deadline = now + 80;
                }
                case 2 ->
                {
                    if (!level.getBlockState(absolute).is(Blocks.WATER))
                    {
                        if (now > state.deadline)
                            state.fail("Mob-built block did not restore after its chunk was unloaded");
                        return;
                    }
                    if (state.hasVisual())
                    {
                        if (now > state.deadline)
                            state.fail("BuildBlockDisappear visual from the unloaded chunk did not expire");
                        return;
                    }

                    actions.build(relative);
                    state.phase = 3;
                    state.deadline = now + 80;
                }
                case 3 ->
                {
                    if (!level.getBlockState(absolute).is(Blocks.WATER) || !state.hasVisual())
                    {
                        if (now > state.deadline)
                            state.fail("Second mob-built block did not produce the BuildBlockDisappear visual");
                        return;
                    }

                    level.setChunkForced(chunk.x(), chunk.z(), false);
                    state.visualUnloadedAt = now;
                    state.deadline = now + 100;
                    state.phase = 4;
                }
                case 4 ->
                {
                    if (!state.loaded()) state.chunkUnloaded = true;
                    if (!state.chunkUnloaded || now < state.visualUnloadedAt + 30)
                    {
                        if (now > state.deadline)
                            state.fail("Chunk containing the BuildBlockDisappear visual did not unload");
                        return;
                    }

                    state.forceLoad();
                    state.phase = 5;
                    state.deadline = now + 20;
                }
                case 5 ->
                {
                    if (!state.loaded() && now <= state.deadline) return;
                    boolean restored = level.getBlockState(absolute).is(Blocks.WATER);
                    boolean staleVisual = state.hasVisual();
                    state.cleanup();
                    helper.assertTrue(restored, "Restored block changed after the visual chunk was reloaded");
                    helper.assertFalse(staleVisual,
                            "BuildBlockDisappear visual remained after its chunk was unloaded and reloaded");
                    helper.succeed();
                }
            }
        });
    }

    public static void brokenBlockRestoresAfterTtlWithoutLoot(GameTestHelper helper)
    {
        RestoringBreakLane lane = restoringBreakLane(helper, Blocks.STONE, "minecraft:stone");
        BlockPos block = lane.world().lowerBarrier();
        BlockPos absolute = helper.absolutePos(block);
        MutableTestValue<Boolean> broken = new MutableTestValue<>(false);

        helper.onEachTick(() ->
        {
            if (lane.world().hasAir(helper))
            {
                broken.set(true);
                helper.assertTrue(
                        WorldGameTest.itemsNear(helper, absolute).stream().noneMatch(item -> item.getItem().is(Items.STONE)),
                        "Restorable block produced loot while absent"
                );
            }
        });
        helper.succeedWhen(() ->
        {
            helper.assertTrue(
                    broken.get(),
                    "Zombie never broke the stone"
            );
            helper.assertTrue(
                    lane.world().is(helper, Blocks.STONE),
                    "Zombie-broken stone did not restore after the configured TTL"
            );
            helper.assertTrue(
                    WorldGameTest.itemsNear(helper, absolute).stream().noneMatch(item -> item.getItem().is(Items.STONE)),
                    "Restoration produced stone loot"
            );
        });
    }

    public static void brokenContainerRestoresBlockEntityNbt(GameTestHelper helper)
    {
        RestoringBreakLane lane = restoringBreakLane(helper, Blocks.CHEST, "minecraft:chest");
        BlockPos lowerRelative = lane.world().lowerBarrier();
        BlockPos lowerAbsolute = helper.absolutePos(lowerRelative);
        BlockPos higherRelative = lane.world().upperBarrier();
        BlockPos higherAbsolute = helper.absolutePos(higherRelative);
        ChestBlockEntity lowerChest = (ChestBlockEntity) helper.getLevel().getBlockEntity(lowerAbsolute);
        ChestBlockEntity higherChest = (ChestBlockEntity) helper.getLevel().getBlockEntity(higherAbsolute);
        lowerChest.setItem(0, new ItemStack(Items.DIAMOND, 3));
        lowerChest.setChanged();
        higherChest.setItem(0, new ItemStack(Items.DIAMOND, 3));
        higherChest.setChanged();
        MutableTestValue<Boolean> lowerBroken = new MutableTestValue<>(false);
        MutableTestValue<Boolean> higherBroken = new MutableTestValue<>(false);

        helper.onEachTick(() ->
        {
            if (WorldGameTest.state(helper, lowerRelative).isAir())
                lowerBroken.set(true);

            if (WorldGameTest.state(helper, higherRelative).isAir())
                higherBroken.set(true);
        });
        helper.succeedWhen(() ->
        {
            helper.assertTrue(
                    lowerBroken.get() || higherBroken.get(),
                    "Zombie never broke the chest"
            );
            helper.assertTrue(
                    lane.world().is(helper, Blocks.CHEST),
                    "Zombie-broken chest barrier did not fully restore after the configured TTL"
            );

            Container lowerRestoredContainer = (Container) helper.getLevel().getBlockEntity(lowerAbsolute);
            Container higherRestoredContainer = (Container) helper.getLevel().getBlockEntity(higherAbsolute);
            boolean isLowerRestored = lowerRestoredContainer != null && lowerRestoredContainer.getItem(0).is(Items.DIAMOND) && lowerRestoredContainer.getItem(0).getCount() == 3;
            boolean isHigherRestored = higherRestoredContainer != null && higherRestoredContainer.getItem(0).is(Items.DIAMOND) && higherRestoredContainer.getItem(0).getCount() == 3;
            boolean isFullyRestored = isHigherRestored && isLowerRestored;

            helper.assertTrue(
                    isFullyRestored,
                    "Restored chest lost its block-entity contents"
            );
        });
    }

    public static void occupiedBrokenPositionPreservesReplacementAndDropsStoredItem(GameTestHelper helper)
    {
        RestoringBreakLane lane = restoringBreakLane(helper, Blocks.STONE, "minecraft:stone");
        BlockPos relative = lane.world().lowerBarrier();
        BlockPos absolute = helper.absolutePos(relative);
        MutableTestValue<Long> replacedAt = new MutableTestValue<>(-1L);
        MutableTestValue<Boolean> droppedStone = new MutableTestValue<>(false);

        helper.onEachTick(() ->
        {
            if (replacedAt.get() >= 0 || !lane.world().hasAir(helper)) return;

            helper.setBlock(relative, Blocks.OBSIDIAN);
            helper.setBlock(lane.world().upperBarrier(), Blocks.OBSIDIAN);
            GameTestEntities.removeServerPlayer(helper, lane.target());
            replacedAt.set(helper.getLevel().getGameTime());
        });
        helper.onEachTick(() ->
        {
            if (WorldGameTest.itemsNear(helper, absolute).stream().anyMatch(item -> item.getItem().is(Items.STONE)))
                droppedStone.set(true);
        });
        helper.succeedWhen(() ->
        {
            helper.assertTrue(
                    replacedAt.get() >= 0 && helper.getLevel().getGameTime() >= replacedAt.get() + 15,
                    "Zombie never created the restorable empty position");
            helper.assertTrue(lane.world().is(helper, Blocks.OBSIDIAN),
                    "Restoration overwrote the externally placed obsidian");
            helper.assertTrue(droppedStone.get(),
                    "The displaced original stone was not dropped");
        });
    }

    public static void fallingBuildDisappearMovesTrackingAndRestoresLandingOldState(GameTestHelper helper)
    {
        configureBuilding(helper);
        GameTestConfig.put(helper, "blocks.mobPlaceBlockIdOverrideList", "minecraft:zombie", "minecraft:sand");
        GameTestConfig.set(helper, "blockRestoration.builtBlocksDisappearing", true);
        GameTestConfig.set(helper, "blockRestoration.builtBlocksDisappearTime", 1.0);
        WorldGameTest.GapLane lane = WorldGameTest.twoBlockGapLane(helper, 2, 3);
        helper.setBlock(lane.firstGap().below(3), Blocks.STONE);
        BlockPos origin = lane.firstGap();
        BlockPos landing = lane.firstGap().below(2);
        MutableTestValue<Boolean> landed = new MutableTestValue<>(false);

        DirectMobActions.forMob(helper, GameTestEntities.frozenZombie(helper, lane.mobStart())).build(origin);
        helper.onEachTick(() ->
        {
            if (WorldGameTest.state(helper, landing).is(Blocks.SAND)) landed.set(true);
        });
        helper.succeedWhen(() ->
        {
            helper.assertTrue(landed.get(), "Mob-built falling sand never reached its landing position");
            helper.assertTrue(WorldGameTest.state(helper, landing).isAir(),
                    "Falling mob-built block did not disappear from its landing position");
        });
    }

    public static void brokenBlocksRestoringDisabledKeepsNormalLoot(GameTestHelper helper)
    {
        GameTestConfig.accelerateActions(helper);
        GameTestConfig.add(helper, "ai.ignoreBuildEntityIdList", "minecraft:zombie");
        GameTestConfig.put(helper, "balance.blockDamage.blockHealthOverrideList", "minecraft:dirt", 1);
        GameTestConfig.set(helper, "blockRestoration.brokenBlocksRestoring", false);
        GameTestConfig.set(helper, "blockRestoration.brokenBlocksRestoreTime", 0.5);
        WorldGameTest.corridor(helper, 0, 6, 2, 1);
        WorldGameTest.barrier(helper, 3, 2, Blocks.DIRT);
        Zombie zombie = GameTestEntities.frozenZombie(helper, new BlockPos(2, 2, 2));
        zombie.setCanPickUpLoot(false);
        BlockPos relative = new BlockPos(3, 2, 2);
        BlockPos absolute = helper.absolutePos(relative);
        MutableTestValue<Long> brokenAt = new MutableTestValue<>(-1L);

        DirectMobActions.forMob(helper, zombie).breakBlock(relative);
        helper.onEachTick(() ->
        {
            if (brokenAt.get() < 0 && WorldGameTest.state(helper, relative).isAir())
                brokenAt.set(helper.getLevel().getGameTime());
        });
        helper.succeedWhen(() ->
        {
            helper.assertTrue(brokenAt.get() >= 0
                            && helper.getLevel().getGameTime() >= brokenAt.get() + 20,
                    "Break action never broke the dirt or the observation interval did not elapse");
            helper.assertTrue(WorldGameTest.state(helper, relative).isAir(),
                    "Broken dirt restored while restoration was disabled");
            helper.assertTrue(WorldGameTest.itemsNear(helper, absolute).stream().anyMatch(item -> item.getItem().is(Items.DIRT)),
                    "Disabled restoration suppressed normal dirt loot");
        });
    }

    public static void builtBlocksDisappearingDisabledKeepsMobBlock(GameTestHelper helper)
    {
        configureBuilding(helper);
        GameTestConfig.set(helper, "blockRestoration.builtBlocksDisappearing", false);
        GameTestConfig.set(helper, "blockRestoration.builtBlocksDisappearTime", 0.5);
        WorldGameTest.GapLane lane = WorldGameTest.twoBlockGapLane(helper, 2, 3);
        BlockPos built = lane.firstGap();
        MutableTestValue<Long> builtAt = new MutableTestValue<>(-1L);

        DirectMobActions.forMob(helper, GameTestEntities.frozenZombie(helper, lane.mobStart())).build(built);
        helper.onEachTick(() ->
        {
            if (builtAt.get() < 0 && WorldGameTest.state(helper, built).is(Blocks.DIRT))
                builtAt.set(helper.getLevel().getGameTime());
        });
        helper.succeedWhen(() ->
        {
            helper.assertTrue(builtAt.get() >= 0
                            && helper.getLevel().getGameTime() >= builtAt.get() + 20,
                    "Zombie never built the block or the observation interval did not elapse");
            helper.assertTrue(WorldGameTest.state(helper, built).is(Blocks.DIRT),
                    "Mob-built block disappeared while disappearance was disabled");
        });
    }

    private static void configureBuilding(GameTestHelper helper)
    {
        GameTestConfig.set(helper, "ai.alwaysSeeNearestPlayer", true);
        GameTestConfig.set(helper, "balance.cooldowns.buildCooldown", 0);
        GameTestConfig.add(helper, "ai.ignoreBreakEntityIdList", "minecraft:zombie");
    }

    private static RestoringBreakLane restoringBreakLane(GameTestHelper helper, Block block, String blockId)
    {
        GameTestConfig.accelerateActions(helper);
        GameTestConfig.add(helper, "ai.ignoreBuildEntityIdList", "minecraft:zombie");
        GameTestConfig.put(helper, "balance.blockDamage.blockHealthOverrideList", blockId, 1);
        GameTestConfig.set(helper, "blockRestoration.brokenBlocksRestoring", true);
        GameTestConfig.set(helper, "blockRestoration.brokenBlocksRestoreTime", 0.5);
        WorldGameTest.BarrierLane lane = WorldGameTest.barrierLane(helper, 2, block);
        ServerPlayer player = GameTestEntities.serverPlayer(helper, lane.target(), GameType.SURVIVAL);
        Zombie zombie = GameTestEntities.zombie(helper, lane.mobStart());
        GameTestEntities.keepTargeting(helper, zombie, player);
        return new RestoringBreakLane(lane, player);
    }

    private record RestoringBreakLane(WorldGameTest.BarrierLane world, ServerPlayer target) {}
}
