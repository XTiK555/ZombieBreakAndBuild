package com.tik.zbb.gametest;

import com.mojang.math.Transformation;
import com.tik.zbb.Constants;
import com.tik.zbb.event.MixinEvents;
import com.tik.zbb.mixin.accessor.MobAccessor;
import com.tik.zbb.mixin.accessor.TargetingConditionsAccessor;
import com.tik.zbb.mixin.accessor.display.BlockDisplayAccessor;
import com.tik.zbb.mixin.accessor.display.DisplayAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.greenrobot.eventbus.Subscribe;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.lang.reflect.Method;

final class MixinCompatibilityGameTests
{
    private MixinCompatibilityGameTests() {}

    static void blockAndAccessors(GameTestHelper helper)
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

        Zombie zombie = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(1, 2, 1));
        MobAccessor mobAccessor = (MobAccessor) zombie;
        helper.assertTrue(mobAccessor.zbb$getGoalSelector() != null, "Mob goalSelector accessor returned null");
        helper.assertTrue(mobAccessor.zbb$getTargetSelector() != null, "Mob targetSelector accessor returned null");
        helper.assertTrue(mobAccessor.zbb$getGoalSelector() != mobAccessor.zbb$getTargetSelector(),
                "Mob selector accessors returned the same field");

        TargetingConditions.Selector selector = (candidate, level) -> true;
        TargetingConditions conditions = TargetingConditions.forCombat().selector(selector);
        helper.assertTrue(((TargetingConditionsAccessor) (Object) conditions).zbb$getSelector() == selector,
                "TargetingConditions selector accessor returned the wrong field");

        Display.BlockDisplay display = helper.spawn(EntityTypes.BLOCK_DISPLAY, new BlockPos(4, 2, 2));
        Transformation transformation = new Transformation(new Vector3f(1, 2, 3), new Quaternionf(),
                new Vector3f(2, 2, 2), new Quaternionf());
        DisplayAccessor displayAccessor = (DisplayAccessor) display;
        displayAccessor.zbb$setTransformation(transformation);
        displayAccessor.zbb$setTransformationInterpolationDuration(7);
        displayAccessor.zbb$setTransformationInterpolationDelay(3);
        ((BlockDisplayAccessor) display).zbb$setBlockState(Blocks.GOLD_BLOCK.defaultBlockState());

        helper.assertTrue(readDisplayTransformation(display).translation().equals(transformation.translation()),
                "Display transformation invoker did not update synced data");
        helper.assertValueEqual(invokePrivateInt(Display.class, display, "getTransformationInterpolationDuration"),
                7, "display interpolation duration");
        helper.assertValueEqual(invokePrivateInt(Display.class, display, "getTransformationInterpolationDelay"),
                3, "display interpolation delay");
        helper.assertTrue(invokePrivateBlockState(display).is(Blocks.GOLD_BLOCK),
                "BlockDisplay state invoker did not update synced data");
        helper.succeed();
    }

    static void fallingBlock(GameTestHelper helper)
    {
        BlockPos floor = new BlockPos(2, 1, 2);
        BlockPos start = new BlockPos(2, 6, 2);
        helper.setBlock(floor, Blocks.STONE);
        FallingObserver observer = new FallingObserver();
        Constants.EVENT_BUS.register(observer);
        FallingBlockEntity entity = FallingBlockEntity.fall(helper.getLevel(), helper.absolutePos(start),
                Blocks.SAND.defaultBlockState());
        helper.assertTrue(observer.started != null && observer.started.entity() == entity,
                "FallingBlock fall injection did not publish the start event");

        helper.succeedWhen(() ->
        {
            if (!entity.isRemoved()) entity.tick();
            helper.assertTrue(entity.isRemoved(), "Falling block has not landed");
            helper.assertTrue(observer.finished != null && observer.finished.entity() == entity,
                    "FallingBlock tick injection did not publish the finish event");
            helper.assertTrue(observer.finished.oldState() != null && observer.finished.oldState().isAir(),
                    "FallingBlock landing injection did not capture the replaced state");
            helper.assertTrue(helper.getLevel().getBlockState(entity.blockPosition()).is(Blocks.SAND),
                    "Falling block did not place its block on landing");
            Constants.EVENT_BUS.unregister(observer);
        });
    }

    static void nearestTargetThroughWall(GameTestHelper helper)
    {
        Zombie zombie = spawnFrozenZombie(helper, new BlockPos(1, 2, 1));
        Villager villager = helper.spawn(EntityTypes.VILLAGER, new BlockPos(5, 2, 1));
        villager.setNoAi(true);
        buildWall(helper);
        helper.assertFalse(zombie.getSensing().hasLineOfSight(villager),
                "AI wall does not block vanilla line of sight");

        NearestAttackableTargetGoal<Villager> goal =
                new NearestAttackableTargetGoal<>(zombie, Villager.class, 0, true, false,
                        (candidate, level) -> candidate == villager);
        helper.succeedWhen(() ->
        {
            helper.assertTrue(goal.canUse(), "Nearest target mixin did not find a target through one solid wall");
            goal.start();
            helper.assertTrue(zombie.getTarget() == villager, "Nearest target goal selected the wrong target");
        });
    }

    static void continueTargetThroughWall(GameTestHelper helper)
    {
        Zombie zombie = spawnFrozenZombie(helper, new BlockPos(1, 2, 1));
        Villager villager = helper.spawn(EntityTypes.VILLAGER, new BlockPos(5, 2, 1));
        villager.setNoAi(true);
        buildWall(helper);
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

    private static Zombie spawnFrozenZombie(GameTestHelper helper, BlockPos pos)
    {
        Zombie zombie = helper.spawn(EntityTypes.ZOMBIE, pos);
        zombie.setNoAi(true);
        zombie.setInvulnerable(true);
        zombie.setTarget(null);
        return zombie;
    }

    private static void buildWall(GameTestHelper helper)
    {
        for (int y = 1; y <= 4; y++) helper.setBlock(new BlockPos(3, y, 1), Blocks.STONE);
    }

    private static Transformation readDisplayTransformation(Display display)
    {
        try
        {
            Method method = Display.class.getDeclaredMethod("createTransformation", SynchedEntityData.class);
            method.setAccessible(true);
            return (Transformation) method.invoke(null, display.getEntityData());
        }
        catch (ReflectiveOperationException exception)
        {
            throw new AssertionError("Cannot inspect Display transformation", exception);
        }
    }

    private static int invokePrivateInt(Class<?> owner, Object target, String name)
    {
        try
        {
            Method method = owner.getDeclaredMethod(name);
            method.setAccessible(true);
            return (int) method.invoke(target);
        }
        catch (ReflectiveOperationException exception)
        {
            throw new AssertionError("Cannot inspect " + name, exception);
        }
    }

    private static BlockState invokePrivateBlockState(Display.BlockDisplay display)
    {
        try
        {
            Method method = Display.BlockDisplay.class.getDeclaredMethod("getBlockState");
            method.setAccessible(true);
            return (BlockState) method.invoke(display);
        }
        catch (ReflectiveOperationException exception)
        {
            throw new AssertionError("Cannot inspect BlockDisplay block state", exception);
        }
    }

    public static final class BlockChangeObserver
    {
        private final BlockPos expectedPos;
        private MixinEvents.OnLevelChunkBlockChangedEvent event;

        private BlockChangeObserver(BlockPos expectedPos)
        {
            this.expectedPos = expectedPos;
        }

        @Subscribe
        public void onBlockChanged(MixinEvents.OnLevelChunkBlockChangedEvent event)
        {
            if (event.pos().equals(expectedPos)) this.event = event;
        }
    }

    public static final class FallingObserver
    {
        private MixinEvents.OnFallingBlockStartedEvent started;
        private MixinEvents.OnFallingBlockFinishedEvent finished;

        @Subscribe
        public void onStarted(MixinEvents.OnFallingBlockStartedEvent event)
        {
            started = event;
        }

        @Subscribe
        public void onFinished(MixinEvents.OnFallingBlockFinishedEvent event)
        {
            finished = event;
        }
    }
}
