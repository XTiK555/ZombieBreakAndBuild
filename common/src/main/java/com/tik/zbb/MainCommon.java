package com.tik.zbb;

import com.tik.zbb.config.ConfigData;
import com.tik.zbb.config.ConfigManager;
import com.tik.zbb.goals.AlwaysSeeNearestPlayerGoal;
import com.tik.zbb.goals.BreakAndBuildGoal;
import com.tik.zbb.goals.ThroughWallsNearestTargetGoal;
import com.tik.zbb.utilities.SecondsToTicksUtility;
import com.tik.zbb.utilities.IMobAccessorMixin;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.player.Player;

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
        if (!shouldApplyTo(mob)) return;
        if (!(mob instanceof IMobAccessorMixin pFMobAccessor)) return;

        AttributeInstance followRangeAttribute = mob.getAttribute(Attributes.FOLLOW_RANGE);
        PathfinderMob pFMob = (PathfinderMob) mob;

        pFMobAccessor.zbb_getGoalSelector().addGoal(2, new BreakAndBuildGoal(pFMob));
        pFMobAccessor.zbb_getTargetSelector().addGoal(2, new AlwaysSeeNearestPlayerGoal(pFMob));

        if (config.isAttackingAllEntities)
        {
            pFMobAccessor.zbb_getTargetSelector().addGoal(1, new ThroughWallsNearestTargetGoal<>(pFMob, LivingEntity.class));
        }
        if (followRangeAttribute != null && followRangeAttribute.getBaseValue() < config.targetSearchRadius)
        {
            followRangeAttribute.setBaseValue(config.targetSearchRadius);
        }
    }

    private static boolean shouldApplyTo(Mob mob)
    {
        boolean isPathFinding = mob instanceof PathfinderMob;
        boolean applyToAllHostiles = config.isApplyingToAllHostiles;
        boolean isHostile = mob.getType().getCategory() == MobCategory.MONSTER;
        boolean isZombie = mob instanceof Zombie || mob instanceof Drowned || mob instanceof Husk || mob instanceof ZombieVillager;

        if (!isPathFinding) return false;
        if (applyToAllHostiles) return isHostile;
        return isZombie;
    }
}