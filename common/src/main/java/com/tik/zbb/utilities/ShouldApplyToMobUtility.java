package com.tik.zbb.utilities;

import com.tik.zbb.config.ConfigSnapshot;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;

public final class ShouldApplyToMobUtility
{
    public static boolean matchesZbbMobFilter(Mob mob, ConfigSnapshot configSnapshot)
    {
        if (!(mob.level() instanceof ServerLevel)) return false;
        if (!(mob instanceof PathfinderMob)) return false;

        Registry<EntityType> entityTypeRegistry = mob.level().registryAccess().lookupOrThrow(Registries.ENTITY_TYPE);
        Identifier entityId = entityTypeRegistry.getKey(mob.getType());

        return configSnapshot.runtime().affectedEntityIdMatcher().matches(entityId);
    }
}
