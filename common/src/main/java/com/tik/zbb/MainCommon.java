package com.tik.zbb;

import com.tik.zbb.config.ConfigData;
import com.tik.zbb.config.ConfigManager;
import com.tik.zbb.ai.goals.AlwaysSeeNearestPlayerGoal;
import com.tik.zbb.ai.goals.BreakAndBuildGoal;
import com.tik.zbb.ai.goals.ThroughWallsNearestTargetGoal;
import com.tik.zbb.mixin.accessor.GoalSelectorAccessor;
import com.tik.zbb.mixin.accessor.MobAccessor;
import com.tik.zbb.utilities.SecondsToTicksUtility;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
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

// This class is part of the common project meaning it is shared between all supported loaders. Code written here can only
// import and access the vanilla codebase, libraries used by vanilla, and optionally third party libraries that provide
// common compatible binaries. This means common code can not directly use loader specific concepts such as Forge events
// however it will be compatible with all supported mod loaders.
public class MainCommon
{
    public static void init()
    {
        ConfigManager.init();
    }

    public static void onLevelTick(ServerLevel level)
    {
        ConfigData config = ConfigManager.getConfigSnapshot().data();
        long now = level.getGameTime();
        int interval = 10;

        if (now % interval == 0)
        {
            long damageTtl = SecondsToTicksUtility.toTicks(config.balance.damageStoreTime);
            long buildTtl = SecondsToTicksUtility.toTicks(config.balance.builtBlocksProtectionTime);

            BlockStorage.cleanUpDamageData(level, damageTtl);
            BlockStorage.cleanUpBuildData(level, buildTtl);
        }
    }

    public static void onJoin(Mob mob)
    {
        ConfigData config = ConfigManager.getConfigSnapshot().data();

        if (!matchesZbbMobFilter(mob, config)) return;
        if (!(mob instanceof MobAccessor pFMobAccessor)) return;

        AttributeInstance followRangeAttribute = mob.getAttribute(Attributes.FOLLOW_RANGE);
        PathfinderMob pFMob = (PathfinderMob) mob;
        GoalSelector targetSelector = pFMobAccessor.zbb$getTargetSelector();
        Set<WrappedGoal> wrapped = ((GoalSelectorAccessor) (Object) targetSelector).zbb$getAvailableGoals();
        List<NearestAttackableTargetGoal<?>> vanillaNatg = new ArrayList<>();
        for (WrappedGoal wg : wrapped)
        {
            Goal g = wg.getGoal();
            if (g instanceof NearestAttackableTargetGoal<?> natg)
            {
                vanillaNatg.add(natg);
            }
        }

        pFMobAccessor.zbb$getGoalSelector().addGoal(2, new BreakAndBuildGoal(pFMob));
        pFMobAccessor.zbb$getTargetSelector().addGoal(2, new AlwaysSeeNearestPlayerGoal(pFMob));
        pFMobAccessor.zbb$getTargetSelector().addGoal(1, new ThroughWallsNearestTargetGoal(pFMob, vanillaNatg));

        if (followRangeAttribute != null && followRangeAttribute.getBaseValue() < config.ai.targetSearchRadius)
        {
            followRangeAttribute.setBaseValue(config.ai.targetSearchRadius);
        }
    }

    private static boolean matchesZbbMobFilter(Mob mob, ConfigData config)
    {
        if (!(mob.level() instanceof ServerLevel)) return false;
        if (!(mob instanceof PathfinderMob)) return false;

        Registry<EntityType> entityTypeRegistry = mob.level().registryAccess().registryOrThrow(Registries.ENTITY_TYPE);
        ResourceLocation entityId = entityTypeRegistry.getKey(mob.getType());

        if (entityId != null && config.ignoreHostileEntityIdSet.contains(entityId)) return false;
        if (entityId != null && config.additionalEntityIdSet.contains(entityId)) return true;
        if (mob.getType().getCategory() != MobCategory.MONSTER) return false;
        if (config.ai.applyToAllMonsters) return true;

        return mob instanceof Zombie || mob instanceof Drowned || mob instanceof Husk || mob instanceof ZombieVillager;
    }
}