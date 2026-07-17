package com.tik.zbb.utilities;

import com.tik.zbb.config.ConfigSnapshot;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ShouldApplyToMobUtility
{
    private static final Object FILTER_CACHE_LOCK = new Object();
    private static volatile FilterCache filterCache = new FilterCache(Long.MIN_VALUE, new ConcurrentHashMap<>());

    public static boolean matchesSimpleZbbMobFilter(Mob mob)
    {
        if (!(mob.level() instanceof ServerLevel)) return false;
        if (!(mob instanceof PathfinderMob)) return false;

        return true;
    }

    public static boolean matchesFullZbbMobFilter(Mob mob, ConfigSnapshot configSnapshot)
    {
        if (!matchesSimpleZbbMobFilter(mob)) return false;

        return matchesAffectedEntityType(mob, configSnapshot);
    }

    private static boolean matchesAffectedEntityType(Mob mob, ConfigSnapshot configSnapshot)
    {
        long requestedVersion = configSnapshot.version();
        FilterCache cache = filterCache;

        if (cache.configVersion() < requestedVersion)
        {
            synchronized (FILTER_CACHE_LOCK)
            {
                cache = filterCache;

                if (cache.configVersion() < requestedVersion)
                {
                    cache = new FilterCache(requestedVersion, new ConcurrentHashMap<>());

                    filterCache = cache;
                }
            }
        }

        // If old snapshot version
        if (cache.configVersion() != requestedVersion)
        {
            return calculateAffectedEntityType(mob, configSnapshot);
        }

        return cache.matchesByType().computeIfAbsent(
                mob.getType(),
                _ -> calculateAffectedEntityType(mob, configSnapshot)
        );
    }

    private static boolean calculateAffectedEntityType(Mob mob, ConfigSnapshot configSnapshot)
    {
        Registry<EntityType> registry = mob.level().registryAccess().lookupOrThrow(Registries.ENTITY_TYPE);

        Identifier entityId = registry.getKey(mob.getType());
        if (entityId == null) return false;

        return configSnapshot.game().ai().affectedEntityIdMatcher().matches(entityId.toString());
    }

    private record FilterCache(long configVersion, ConcurrentMap<EntityType<?>, Boolean> matchesByType) {}
}
