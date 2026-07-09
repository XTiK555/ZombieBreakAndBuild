package com.tik.zbb.config;

import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record ConfigSnapshot(ConfigData data, ConfigRuntime runtime, long version)
{
    public static ConfigSnapshot create(ConfigData data, long version)
    {
        return new ConfigSnapshot(data, ConfigRuntime.create(data), version);
    }

    public record ConfigRuntime(
            Set<Identifier> dangerousBlockIdSet,
            Set<Identifier> ignoreBuildEntityIdSet,
            Set<Identifier> ignoreBreakEntityIdSet,
            Set<Identifier> additionalEntityIdSet,
            Map<Identifier, Identifier> dimensionPlaceBlockIdMap,
            Map<Identifier, Identifier> mobPlaceBlockIdOverrideMap,
            Map<Identifier, Integer> blockHealthOverrideMap
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

        private static Set<Identifier> idListToSet(List<String> list)
        {
            Set<Identifier> set = new HashSet<>();

            for (String s : list)
            {
                Identifier id = Identifier.tryParse(s);
                if (id != null) set.add(id);
            }

            return Set.copyOf(set);
        }

        private static Map<Identifier, Identifier> idPairListToMap(List<String> list)
        {
            Map<Identifier, Identifier> map = new HashMap<>();

            for (String s : list)
            {
                String[] parts = s.split("=", 2);
                if (parts.length != 2) continue;

                Identifier key = Identifier.tryParse(parts[0].trim());
                Identifier value = Identifier.tryParse(parts[1].trim());
                if (key != null && value != null) map.put(key, value);
            }

            return Map.copyOf(map);
        }

        private static Map<Identifier, Integer> idIntPairListToMap(List<String> list)
        {
            Map<Identifier, Integer> map = new HashMap<>();

            for (String s : list)
            {
                String[] parts = s.split("=", 2);
                if (parts.length != 2) continue;

                Identifier key = Identifier.tryParse(parts[0].trim());
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
}
