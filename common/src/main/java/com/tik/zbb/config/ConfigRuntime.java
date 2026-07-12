package com.tik.zbb.config;

import com.tik.zbb.config.schema.ResourceLocationPatternMatcher;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record ConfigRuntime(
        ResourceLocationPatternMatcher dangerousBlockIdMatcher,
        ResourceLocationPatternMatcher affectedEntityIdMatcher,
        ResourceLocationPatternMatcher ignoreBuildEntityIdMatcher,
        ResourceLocationPatternMatcher ignoreBreakEntityIdMatcher,
        Map<ResourceLocation, ResourceLocation> dimensionPlaceBlockIdMap,
        Map<ResourceLocation, ResourceLocation> mobPlaceBlockIdOverrideMap,
        Map<ResourceLocation, Integer> blockHealthOverrideMap
)
{
    public static ConfigRuntime create(ConfigData data)
    {
        return new ConfigRuntime(
                ResourceLocationPatternMatcher.compile(data.blocks.dangerousBlockIdList),
                ResourceLocationPatternMatcher.compile(data.ai.affectedEntityIdList),
                ResourceLocationPatternMatcher.compile(data.ai.ignoreBuildEntityIdList),
                ResourceLocationPatternMatcher.compile(data.ai.ignoreBreakEntityIdList),
                idPairListToMap(data.blocks.dimensionPlaceBlockIdList),
                idPairListToMap(data.blocks.mobPlaceBlockIdOverrideList),
                idIntPairListToMap(data.balance.blockDamage.blockHealthOverrideList)
        );
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
