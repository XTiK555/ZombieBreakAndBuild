package com.tik.zbb;

import com.tik.zbb.ai.goals.AlwaysSeeNearestPlayerGoal;
import com.tik.zbb.ai.goals.BreakAndBuildGoal;
import com.tik.zbb.blockstorage.BlockStorages;
import com.tik.zbb.config.ConfigData;
import com.tik.zbb.config.ConfigManager;
import com.tik.zbb.event.EventRegistrar;
import com.tik.zbb.mixin.accessor.MobAccessor;
import com.tik.zbb.utilities.ShouldApplyToMobUtility;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;

import static com.tik.zbb.utilities.SecondsToTicksUtility.toTicks;

public class MainCommon
{
    public static void init()
    {
        ConfigManager.init();
        EventRegistrar.registerAll();
    }

    public static void onLevelTick(ServerLevel level)
    {
        ConfigData config = ConfigManager.getConfigSnapshot().data();

        BlockStorages.BUILD_PROTECTION_MANAGER.cleanup(level, toTicks(config.balance.builtBlocksProtectionTime));
        BlockStorages.DAMAGE_MANAGER.cleanup(level, toTicks(config.balance.damageStoreTime));
        if (config.blockRestoration.brokenBlocksRestoring)
            BlockStorages.BROKEN_MANAGER.cleanup(level, toTicks(config.blockRestoration.brokenBlocksRestoreTime));
        if (config.blockRestoration.builtBlocksDisappearing)
            BlockStorages.BUILD_DISAPPEAR_MANAGER.cleanup(level, toTicks(config.blockRestoration.builtBlocksDisappearTime));
    }

    public static void onServerTick(MinecraftServer server)
    {
        Constants.SCHEDULER.tick();
    }

    public static void onServerStopping(MinecraftServer server)
    {
        Constants.SCHEDULER.clear();
        Constants.EVENT_BUS.post(new OnServerStoppingEvent(server));
    }

    public static void onJoin(Mob mob)
    {
        ConfigData config = ConfigManager.getConfigSnapshot().data();

        if (!ShouldApplyToMobUtility.matchesZbbMobFilter(mob, config)) return;
        if (!(mob instanceof MobAccessor mobAccessor)) return;

        PathfinderMob pFMob = (PathfinderMob) mob;
        GoalSelector targetSelector = mobAccessor.zbb$getTargetSelector();
        GoalSelector goalSelector = mobAccessor.zbb$getGoalSelector();

        if (!hasGoal(goalSelector, BreakAndBuildGoal.class))
            goalSelector.addGoal(2, new BreakAndBuildGoal(pFMob));
        if (!hasGoal(targetSelector, AlwaysSeeNearestPlayerGoal.class))
        {
            int maxTargetPriority = 0;

            for (WrappedGoal wg : (targetSelector.getAvailableGoals()))
            {
                maxTargetPriority = Math.max(maxTargetPriority, wg.getPriority());
            }

            targetSelector.addGoal(maxTargetPriority + 1, new AlwaysSeeNearestPlayerGoal(pFMob));
        }
    }

    private static boolean hasGoal(GoalSelector selector, Class<? extends Goal> goalClass)
    {
        for (WrappedGoal wrapped : selector.getAvailableGoals())
        {
            if (goalClass.isInstance(wrapped.getGoal()))
            {
                return true;
            }
        }
        return false;
    }

    public record OnServerStoppingEvent(MinecraftServer server) {}
}