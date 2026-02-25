package com.tik.zbb.utilities;

import com.tik.zbb.config.ConfigData;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombieVillager;

public final class ShouldApplyToMobUtility
{
    public static boolean shouldAttachZbbGoals(Mob mob, ConfigData config)
    {
        if (!matchesZbbMobFilter(mob, config)) return false;

        return true;
    }

    public static boolean shouldSeeTargetsThroughWalls(Mob mob, ConfigData config)
    {
        if (!matchesZbbMobFilter(mob, config)) return false;
        if (!config.canSeeTargetsThroughBlocks) return false;

        return true;
    }

    public static boolean shouldAlwaysSeeNearestPlayer(Mob mob, ConfigData config)
    {
        if (!matchesZbbMobFilter(mob, config)) return false;
        if (!config.alwaysSeeNearestPlayer) return false;

        return true;
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
        if (config.applyToAllMonsters) return true;

        return mob instanceof Zombie || mob instanceof Drowned || mob instanceof Husk || mob instanceof ZombieVillager;
    }
}
