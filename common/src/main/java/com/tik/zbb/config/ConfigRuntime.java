package com.tik.zbb.config;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record ConfigRuntime(
        Set<ResourceLocation> dangerousBlockIdSet,
        Set<ResourceLocation> ignoreBuildEntityIdSet,
        Set<ResourceLocation> ignoreBreakEntityIdSet,
        Set<ResourceLocation> additionalEntityIdSet,
        Map<ResourceLocation, ResourceLocation> dimensionPlaceBlockIdMap,
        Map<ResourceLocation, ResourceLocation> mobPlaceBlockIdOverrideMap,
        Map<ResourceLocation, Integer> blockHealthOverrideMap
)
{
    public static ConfigRuntime create(ConfigData data)
    {
        return new ConfigRuntime(
                idListToSet(data.blocks.dangerousBlockIdList),
                idListToSet(data.ai.ignoreBuildEntityIdList),
                idListToSet(data.ai.ignoreBreakEntityIdList),
                idListToSet(data.ai.additionalEntityIdList),
                idPairListToMap(data.blocks.dimensionPlaceBlockIdList),
                idPairListToMap(data.blocks.mobPlaceBlockIdOverrideList),
                idIntPairListToMap(data.balance.blockDamage.blockHealthOverrideList)
        );
    }

    private static Set<ResourceLocation> idListToSet(List<String> list)
    {
        Set<ResourceLocation> set = new HashSet<>();

        for (String s : list)
        {
            ResourceLocation id = ResourceLocation.tryParse(s);
            if (id != null) set.add(id);
        }

        return Set.copyOf(set);
    }

    private static Map<ResourceLocation, ResourceLocation> idPairListToMap(List<String> list)
    {
        Map<ResourceLocation, ResourceLocation> map = new HashMap<>();

        for (String s : list)
        {
            String[] parts = s.split("=", 2);
            if (parts.length != 2) continue;

            ResourceLocation key = ResourceLocation.tryParse(parts[0].trim());
            ResourceLocation value = ResourceLocation.tryParse(parts[1].trim());
            if (key != null && value != null) map.put(key, value);
        }

        return Map.copyOf(map);
    }

    private static Map<ResourceLocation, Integer> idIntPairListToMap(List<String> list)
    {
        Map<ResourceLocation, Integer> map = new HashMap<>();

        for (String s : list)
        {
            String[] parts = s.split("=", 2);
            if (parts.length != 2) continue;

            ResourceLocation key = ResourceLocation.tryParse(parts[0].trim());
            if (key == null) continue;

            try
            {
                int value = Integer.parseInt(parts[1].trim());
                if (value >= 0) map.put(key, value);
            }
            catch (NumberFormatException ignored)
            {
            }
        }

        return Map.copyOf(map);
    }
}
