package com.tik.zbb;

import com.mojang.brigadier.CommandDispatcher;
import com.tik.zbb.ai.goals.AlwaysSeeNearestPlayerGoal;
import com.tik.zbb.ai.goals.BreakAndBuildGoal;
import com.tik.zbb.blockstorage.BlockStorages;
import com.tik.zbb.command.ZbbConfigCommand;
import com.tik.zbb.config.ConfigGame;
import com.tik.zbb.config.ConfigManager;
import com.tik.zbb.event.EventRegistrar;
import com.tik.zbb.mixin.accessor.MobAccessor;
import com.tik.zbb.utilities.ShouldApplyToMobUtility;
import net.minecraft.commands.CommandSourceStack;
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
    private static boolean isInitialized = false;

    public static void init()
    {
        if (isInitialized) return;

        ConfigManager.init();
        EventRegistrar.registerAll();

        isInitialized = true;
    }

    public static void onLevelTickPost(ServerLevel level)
    {
        ConfigGame config = ConfigManager.getConfigSnapshot().game();

        BlockStorages.BUILD_PROTECTION_MANAGER.cleanup(level, toTicks(config.balance().builtBlocksProtectionTime()));
        BlockStorages.DAMAGE_MANAGER.cleanup(level, toTicks(config.balance().damageStoreTime()));
        BlockStorages.BROKEN_MANAGER.cleanup(level, toTicks(config.blockRestoration().brokenBlocksRestoreTime()));
        BlockStorages.BUILD_DISAPPEAR_MANAGER.cleanup(level, toTicks(config.blockRestoration().builtBlocksDisappearTime()));
    }

    public static void onServerTickPre(MinecraftServer server)
    {
        Constants.SCHEDULER.tick();
    }

    public static void onServerStarting(MinecraftServer server)
    {
        ConfigManager.startRuntime(server);
    }

    public static void onServerStopping(MinecraftServer server)
    {
        Constants.SCHEDULER.clear();
        Constants.EVENT_BUS.post(new OnServerStoppingEvent(server));
    }

    public static void onJoin(Mob mob)
    {
        if (!ShouldApplyToMobUtility.matchesSimpleZbbMobFilter(mob)) return;
        if (!(mob instanceof MobAccessor mobAccessor)) return;

        PathfinderMob pFMob = (PathfinderMob) mob;
        GoalSelector targetSelector = mobAccessor.zbb$getTargetSelector();
        GoalSelector goalSelector = mobAccessor.zbb$getGoalSelector();

        if (!hasGoal(goalSelector, BreakAndBuildGoal.class))
        {
            goalSelector.addGoal(2, new BreakAndBuildGoal(pFMob));
        }
        if (!hasGoal(targetSelector, AlwaysSeeNearestPlayerGoal.class))
        {
            targetSelector.addGoal(Integer.MAX_VALUE - 1, new AlwaysSeeNearestPlayerGoal(pFMob));
        }
    }

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        ZbbConfigCommand.register(dispatcher);
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
