package com.tik.zbb.utilities;

import com.tik.zbb.config.ConfigData;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;

public final class ShouldApplyToMobUtility
{
    public static boolean matchesZbbMobFilter(Mob mob, ConfigData config)
    {
        if (!(mob.level() instanceof ServerLevel)) return false;
        if (!(mob instanceof PathfinderMob)) return false;

        Registry<EntityType> entityTypeRegistry = mob.level().registryAccess().lookupOrThrow(Registries.ENTITY_TYPE);
        Identifier entityId = entityTypeRegistry.getKey(mob.getType());

        if (entityId != null && config.additionalEntityIdSet.contains(entityId)) return true;
        if (entityId != null && config.ignoreBuildEntityIdSet.contains(entityId) && config.ignoreBreakEntityIdSet.contains(entityId))
            return false;
        if (mob.getType().getCategory() != MobCategory.MONSTER) return false;
        if (config.ai.applyToAllMonsters) return true;

        return mob instanceof Zombie || mob instanceof Drowned || mob instanceof Husk || mob instanceof ZombieVillager;
    }
}
