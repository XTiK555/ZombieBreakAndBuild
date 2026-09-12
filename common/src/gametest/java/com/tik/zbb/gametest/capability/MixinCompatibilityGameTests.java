package com.tik.zbb.gametest.capability;

import com.mojang.math.Transformation;
import com.tik.zbb.Constants;
import com.tik.zbb.event.MixinEvents;
import com.tik.zbb.gametest.fixture.DisplayProbes;
import com.tik.zbb.gametest.fixture.GameTestConfig;
import com.tik.zbb.gametest.fixture.GameTestEntities;
import com.tik.zbb.gametest.fixture.WorldGameTest;
import com.tik.zbb.mixin.accessor.MobAccessor;
import com.tik.zbb.mixin.accessor.TargetingConditionsAccessor;
import com.tik.zbb.mixin.accessor.display.BlockDisplayAccessor;
import com.tik.zbb.mixin.accessor.display.DisplayAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.greenrobot.eventbus.Subscribe;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.function.Predicate;

public final class MixinCompatibilityGameTests
{
    private MixinCompatibilityGameTests() {}

    public static void levelChunkBlockChangeEvent(GameTestHelper helper)
    {
        BlockPos changedPos = new BlockPos(2, 2, 2);
        BlockChangeObserver observer = new BlockChangeObserver(helper.absolutePos(changedPos));
        Constants.EVENT_BUS.register(observer);
        try
        {
            helper.setBlock(changedPos, Blocks.STONE);
            helper.assertTrue(observer.event != null, "LevelChunk mixin did not publish its block-change event");
            helper.assertTrue(observer.event.oldState().isAir(), "Block-change event has the wrong old state");
            helper.assertTrue(observer.event.newState().is(Blocks.STONE), "Block-change event has the wrong new state");
        }
        finally
        {
            Constants.EVENT_BUS.unregister(observer);
        }
        helper.succeed();
    }

    public static void sameBlockTypeStateChangeDoesNotPublishBlockChangedEvent(GameTestHelper helper)
    {
        BlockPos changedPos = new BlockPos(2, 2, 2);
        BlockChangeObserver observer = new BlockChangeObserver(helper.absolutePos(changedPos));
        Constants.EVENT_BUS.register(observer);
        try
        {
            helper.setBlock(changedPos, Blocks.REDSTONE_LAMP.defaultBlockState());
            observer.count = 0;
            helper.setBlock(changedPos, Blocks.REDSTONE_LAMP.defaultBlockState().setValue(BlockStateProperties.LIT, true));
            helper.assertValueEqual(observer.count, 0, "Same-block property change published a block-change event");
            helper.setBlock(changedPos, Blocks.STONE);
            helper.assertValueEqual(observer.count, 1, "Different-block change did not publish exactly one event");
        }
        finally
        {
            Constants.EVENT_BUS.unregister(observer);
        }
        helper.succeed();
    }

    public static void mobAccessors(GameTestHelper helper)
    {
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
        MobAccessor mobAccessor = (MobAccessor) zombie;
        helper.assertTrue(mobAccessor.zbb$getGoalSelector() != null, "Mob goalSelector accessor returned null");
        helper.assertTrue(mobAccessor.zbb$getTargetSelector() != null, "Mob targetSelector accessor returned null");
        helper.assertTrue(mobAccessor.zbb$getGoalSelector() != mobAccessor.zbb$getTargetSelector(),
                "Mob selector accessors returned the same field");
        helper.succeed();
    }

    public static void targetingConditionsAccessor(GameTestHelper helper)
    {
        Predicate<net.minecraft.world.entity.LivingEntity> selector = candidate -> true;
        TargetingConditions conditions = TargetingConditions.forCombat().selector(selector);
        helper.assertTrue(((TargetingConditionsAccessor) (Object) conditions).zbb$getSelector() == selector,
                "TargetingConditions selector accessor returned the wrong field");
        helper.succeed();
    }

    public static void displayAccessors(GameTestHelper helper)
    {
        Display.BlockDisplay display = helper.spawn(EntityType.BLOCK_DISPLAY, new BlockPos(4, 2, 2));
        Transformation transformation = new Transformation(new Vector3f(1, 2, 3), new Quaternionf(),
                new Vector3f(2, 2, 2), new Quaternionf());
        DisplayAccessor displayAccessor = (DisplayAccessor) display;
        displayAccessor.zbb$setTransformation(transformation);
        displayAccessor.zbb$setTransformationInterpolationDuration(7);
        displayAccessor.zbb$setTransformationInterpolationDelay(3);
        ((BlockDisplayAccessor) display).zbb$setBlockState(Blocks.GOLD_BLOCK.defaultBlockState());

        helper.assertTrue(DisplayProbes.transformation(display).getTranslation().equals(transformation.getTranslation()),
                "Display transformation invoker did not update synced data");
        helper.assertValueEqual(DisplayProbes.transformationInterpolationDuration(display),
                7, "display interpolation duration");
        helper.assertValueEqual(DisplayProbes.transformationInterpolationDelay(display),
                3, "display interpolation delay");
        helper.assertTrue(DisplayProbes.blockState(display).is(Blocks.GOLD_BLOCK),
                "BlockDisplay state invoker did not update synced data");
        helper.succeed();
    }

    public static void fallingBlock(GameTestHelper helper)
    {
        BlockPos floor = new BlockPos(2, 1, 2);
        BlockPos start = new BlockPos(2, 6, 2);
        helper.setBlock(floor, Blocks.STONE);
        FallingObserver observer = new FallingObserver();
        Constants.EVENT_BUS.register(observer);
        observer.registered = true;
        helper.runAtTickTime(110, observer::unregister);
        FallingBlockEntity entity;
        try
        {
            entity = FallingBlockEntity.fall(helper.getLevel(), helper.absolutePos(start), Blocks.SAND.defaultBlockState());
        }
        catch (RuntimeException | Error exception)
        {
            observer.unregister();
            throw exception;
        }
        helper.assertTrue(observer.started != null && observer.started.entity() == entity,
                "FallingBlock fall injection did not publish the start event");

        helper.succeedWhen(() ->
        {
            helper.assertTrue(entity.isRemoved(), "Falling block has not landed");
            helper.assertTrue(observer.finished != null && observer.finished.entity() == entity,
                    "FallingBlock tick injection did not publish the finish event");
            helper.assertTrue(observer.finished.oldState() != null && observer.finished.oldState().isAir(),
                    "FallingBlock landing injection did not capture the replaced state");
            helper.assertTrue(helper.getLevel().getBlockState(entity.blockPosition()).is(Blocks.SAND),
                    "Falling block did not place its block on landing");
            observer.unregister();
        });
    }

    public static void nearestTargetThroughWall(GameTestHelper helper)
    {
        GameTestConfig.set(helper, "ai.canNoticeTargetsThroughBlocks", true);
        GameTestConfig.set(helper, "ai.noticeTargetsThroughBlocksLimit", 1);
        GameTestConfig.clear(helper, "ai.affectedEntityIdList");
        GameTestConfig.add(helper, "ai.affectedEntityIdList", "minecraft:zombie");

        Zombie zombie = GameTestEntities.frozenZombie(helper, new BlockPos(1, 2, 1));
        Villager villager = helper.spawn(EntityType.VILLAGER, new BlockPos(5, 2, 1));
        villager.setNoAi(true);
        WorldGameTest.column(helper, 3, 1, 4, 1, Blocks.STONE);
        NearestAttackableTargetGoal<Villager> goal = new NearestAttackableTargetGoal<>(zombie, Villager.class, 0, true, false, candidate -> candidate == villager);

        helper.assertFalse(zombie.getSensing().hasLineOfSight(villager), "AI wall does not block vanilla line of sight");

        helper.succeedWhen(() ->
        {
            helper.assertTrue(goal.canUse(), "Nearest target mixin did not find a target through one solid wall");

            goal.start();

            helper.assertTrue(
                    zombie.getTarget() == villager,
                    "Nearest target goal selected the wrong target"
            );
        });
    }

    public static void nearestTargetThroughWallPreservesOriginalSelector(GameTestHelper helper)
    {
        GameTestConfig.set(helper, "ai.canNoticeTargetsThroughBlocks", true);
        GameTestConfig.set(helper, "ai.noticeTargetsThroughBlocksLimit", 1);
        GameTestConfig.clear(helper, "ai.affectedEntityIdList");
        GameTestConfig.add(helper, "ai.affectedEntityIdList", "minecraft:zombie");

        Zombie zombie = GameTestEntities.frozenZombie(helper, new BlockPos(1, 2, 1));
        Villager villager = helper.spawn(EntityType.VILLAGER, new BlockPos(5, 2, 1));
        villager.setNoAi(true);
        WorldGameTest.column(helper, 3, 1, 4, 1, Blocks.STONE);
        var acceptingGoal = new NearestAttackableTargetGoal<>(
                zombie, Villager.class, 0, true, false, candidate -> candidate == villager);
        var rejectingGoal = new NearestAttackableTargetGoal<>(
                zombie, Villager.class, 0, true, false, candidate -> false);

        helper.succeedWhen(() ->
        {
            helper.assertTrue(acceptingGoal.canUse(),
                    "Nearest-target mixin was not active for the selector-preservation test");
            helper.assertFalse(rejectingGoal.canUse(),
                    "Nearest-target mixin discarded the original selector");
        });
    }

    public static void continueTargetThroughWall(GameTestHelper helper)
    {
        GameTestConfig.set(helper, "ai.canContinueSeeingTargetsThroughBlocks", true);
        GameTestConfig.set(helper, "ai.continueSeeingTargetsThroughBlocksLimit", 1);
        GameTestConfig.clear(helper, "ai.affectedEntityIdList");
        GameTestConfig.add(helper, "ai.affectedEntityIdList", "minecraft:zombie");

        Zombie zombie = GameTestEntities.frozenZombie(helper, new BlockPos(1, 2, 1));
        Villager villager = helper.spawn(EntityType.VILLAGER, new BlockPos(5, 2, 1));
        villager.setNoAi(true);
        WorldGameTest.column(helper, 3, 1, 4, 1, Blocks.STONE);
        helper.assertFalse(zombie.getSensing().hasLineOfSight(villager),
                "AI wall does not block vanilla line of sight");
        zombie.setTarget(villager);

        TargetGoal goal = new TargetGoal(zombie, true)
        {
            @Override
            public boolean canUse()
            {
                return true;
            }
        }.setUnseenMemoryTicks(0);
        goal.start();
        helper.assertTrue(goal.canContinueToUse(),
                "TargetGoal mixin did not preserve a valid target through one solid wall");
        helper.succeed();
    }

    public static final class BlockChangeObserver
    {
        private final BlockPos expectedPos;
        private MixinEvents.OnLevelChunkBlockChangedEvent event;
        private int count;

        private BlockChangeObserver(BlockPos expectedPos)
        {
            this.expectedPos = expectedPos;
        }

        @Subscribe
        public void onBlockChanged(MixinEvents.OnLevelChunkBlockChangedEvent event)
        {
            if (!event.pos().equals(expectedPos)) return;
            this.event = event;
            count++;
        }
    }

    public static final class FallingObserver
    {
        private MixinEvents.OnFallingBlockStartedEvent started;
        private MixinEvents.OnFallingBlockFinishedEvent finished;
        private boolean registered;

        @Subscribe
        public void onStarted(MixinEvents.OnFallingBlockStartedEvent event)
        {
            if (started == null) started = event;
        }

        @Subscribe
        public void onFinished(MixinEvents.OnFallingBlockFinishedEvent event)
        {
            if (started == null || event.entity() != started.entity()) return;
            finished = event;
            unregister();
        }

        private void unregister()
        {
            if (!registered) return;
            registered = false;
            Constants.EVENT_BUS.unregister(this);
        }
    }
}
