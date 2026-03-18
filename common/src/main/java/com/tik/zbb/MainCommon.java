package com.tik.zbb;

import com.tik.zbb.blockstorage.BlockStorages;
import com.tik.zbb.config.ConfigData;
import com.tik.zbb.config.ConfigManager;
import com.tik.zbb.ai.goals.AlwaysSeeNearestPlayerGoal;
import com.tik.zbb.ai.goals.BreakAndBuildGoal;
import com.tik.zbb.mixin.accessor.GoalSelectorAccessor;
import com.tik.zbb.mixin.accessor.MobAccessor;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombieVillager;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.tik.zbb.utilities.SecondsToTicksUtility.toTicks;

public class MainCommon
{
    public static void init()
    {
        ConfigManager.init();
        BlockStorages.init();
    }

    public static void onLevelTick(ServerLevel level)
    {
        ConfigData config = ConfigManager.getConfigSnapshot().data();

        long now = level.getGameTime();
        int interval = 5;

        BlockStorages.BUILD_PROTECTION.cleanup(level, toTicks(config.balance.builtBlocksProtectionTime));
        if (now % interval == 0)
        {
            BlockStorages.DAMAGE.cleanup(level, toTicks(config.balance.optimization.damageStoreTime));
            if (config.blockReturning.brokenBlocksRestoring)
                BlockStorages.BROKEN.cleanup(level, toTicks(config.blockReturning.brokenBlocksRestoreTime));
            if (config.blockReturning.builtBlocksDisappearing)
                BlockStorages.BUILD_DISAPPEAR.cleanup(level, toTicks(config.blockReturning.builtBlocksDisappearTime));
        }
    }

    public static void onJoin(Mob mob)
    {
        ConfigData config = ConfigManager.getConfigSnapshot().data();

        if (!matchesZbbMobFilter(mob, config)) return;
        if (!(mob instanceof MobAccessor mobAccessor)) return;

        PathfinderMob pFMob = (PathfinderMob) mob;
        GoalSelector targetSelector = mobAccessor.zbb$getTargetSelector();
        GoalSelector goalSelector = mobAccessor.zbb$getGoalSelector();

        if (!hasGoal(goalSelector, BreakAndBuildGoal.class))
            goalSelector.addGoal(2, new BreakAndBuildGoal(pFMob));
        if (!hasGoal(targetSelector, AlwaysSeeNearestPlayerGoal.class))
        {
            int maxTargetPriority = 0;

            for (WrappedGoal wg : ((GoalSelectorAccessor) (Object) targetSelector).zbb$getAvailableGoals())
            {
                maxTargetPriority = Math.max(maxTargetPriority, wg.getPriority());
            }

            targetSelector.addGoal(maxTargetPriority + 1, new AlwaysSeeNearestPlayerGoal(pFMob));
        }
    }

    private static boolean matchesZbbMobFilter(Mob mob, ConfigData config)
    {
        if (!(mob.level() instanceof ServerLevel)) return false;
        if (!(mob instanceof PathfinderMob)) return false;

        Registry<EntityType> entityTypeRegistry = mob.level().registryAccess().registryOrThrow(Registries.ENTITY_TYPE);
        ResourceLocation entityId = entityTypeRegistry.getKey(mob.getType());

        if (entityId != null && config.additionalEntityIdSet.contains(entityId)) return true;
        if (entityId != null && config.ignoreBuildEntityIdSet.contains(entityId) && config.ignoreBreakEntityIdSet.contains(entityId))
            return false;
        if (mob.getType().getCategory() != MobCategory.MONSTER) return false;
        if (config.ai.applyToAllMonsters) return true;

        return mob instanceof Zombie || mob instanceof Drowned || mob instanceof Husk || mob instanceof ZombieVillager;
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
}