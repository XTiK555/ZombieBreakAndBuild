package com.tik.zbb;

import com.tik.zbb.config.ConfigData;
import com.tik.zbb.config.ConfigManager;
import com.tik.zbb.goals.AlwaysSeeNearestPlayerGoal;
import com.tik.zbb.goals.BreakAndBuildGoal;
import com.tik.zbb.goals.ThroughWallsNearestTargetGoal;
import com.tik.zbb.utilities.SecondsToTicksUtility;
import com.tik.zbb.utilities.IMobAccessorMixin;
import com.tik.zbb.utilities.ShouldApplyToMobUtility;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

// This class is part of the common project meaning it is shared between all supported loaders. Code written here can only
// import and access the vanilla codebase, libraries used by vanilla, and optionally third party libraries that provide
// common compatible binaries. This means common code can not directly use loader specific concepts such as Forge events
// however it will be compatible with all supported mod loaders.
public class MainCommon
{
    private static ConfigData config;

    public static void init()
    {
        ConfigManager.init(Constants.MOD_NAME + ".json");
        config = ConfigManager.getConfigData();
    }

    public static void onLevelTick(ServerLevel level)
    {
        long now = level.getGameTime();
        int interval = 10;

        if (now % interval == 0)
        {
            long damageTtl = SecondsToTicksUtility.toTicks(config.damageStoreTime);
            long buildTtl = SecondsToTicksUtility.toTicks(config.builtBlocksProtectionTime);

            BlockStorage.cleanUpDamageData(level, damageTtl);
            BlockStorage.cleanUpBuildData(level, buildTtl);
        }
    }

    public static void onJoin(Mob mob)
    {
        if (!ShouldApplyToMobUtility.shouldAttachZbbGoals(mob, config)) return;
        if (!(mob instanceof IMobAccessorMixin pFMobAccessor)) return;

        AttributeInstance followRangeAttribute = mob.getAttribute(Attributes.FOLLOW_RANGE);
        PathfinderMob pFMob = (PathfinderMob) mob;

        pFMobAccessor.zbb_getGoalSelector().addGoal(2, new BreakAndBuildGoal(pFMob));
        pFMobAccessor.zbb_getTargetSelector().addGoal(2, new AlwaysSeeNearestPlayerGoal(pFMob));

        if (config.attackAllEntities)
        {
            pFMobAccessor.zbb_getTargetSelector().addGoal(1, new ThroughWallsNearestTargetGoal<>(pFMob, LivingEntity.class));
        }
        if (followRangeAttribute != null && followRangeAttribute.getBaseValue() < config.targetSearchRadius)
        {
            followRangeAttribute.setBaseValue(config.targetSearchRadius);
        }
    }
}