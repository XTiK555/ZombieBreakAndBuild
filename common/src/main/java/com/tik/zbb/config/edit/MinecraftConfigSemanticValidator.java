package com.tik.zbb.config.edit;

import com.tik.zbb.config.annotations.ResourceLocationRegistry;
import com.tik.zbb.config.annotations.ResourceLocationSemantics;
import com.tik.zbb.config.schema.ConfigFieldDescriptor;
import com.tik.zbb.config.schema.ConfigRepairReport;
import com.tik.zbb.config.schema.ConfigValidationException;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MinecraftConfigSemanticValidator implements ConfigSemanticValidator
{
    public static final MinecraftConfigSemanticValidator INSTANCE = new MinecraftConfigSemanticValidator();

    private MinecraftConfigSemanticValidator() {}

    @Override
    public void validate(ConfigFieldDescriptor descriptor, Object value) throws ConfigValidationException
    {
        ResourceLocationSemantics semantics = descriptor.resourceLocationSemantics();
        if (semantics == null) return;

        if (semantics.element() != ResourceLocationRegistry.NONE)
        {
            requireExactPatternsExist(value, semantics.element());
        }

        if (value instanceof Map<?, ?> map)
        {
            requireMapEntries(map, semantics);
        }
        else if (semantics.value() != ResourceLocationRegistry.NONE)
        {
            requireExists(String.valueOf(value), semantics.value());
        }
    }

    @Override
    public Object repairValue(
            ConfigFieldDescriptor descriptor,
            Object value,
            Object defaultValue,
            ConfigRepairReport report
    )
    {
        ResourceLocationSemantics semantics = descriptor.resourceLocationSemantics();
        if (semantics == null) return value;

        if (semantics.element() != ResourceLocationRegistry.NONE)
        {
            return repairExactPatterns(descriptor, value, semantics.element(), defaultValue, report);
        }

        if (value instanceof Map<?, ?> map)
        {
            return repairMapEntries(descriptor, map, semantics, value, report);
        }

        return ConfigSemanticValidator.super.repairValue(descriptor, value, defaultValue, report);
    }

    private static void requireMapEntries(Map<?, ?> map, ResourceLocationSemantics semantics) throws ConfigValidationException
    {
        for (Map.Entry<?, ?> entry : map.entrySet())
        {
            if (semantics.key() != ResourceLocationRegistry.NONE)
            {
                requireExists(String.valueOf(entry.getKey()), semantics.key());
            }
            if (semantics.value() != ResourceLocationRegistry.NONE)
            {
                requireExists(String.valueOf(entry.getValue()), semantics.value());
            }
        }
    }

    private static Object repairMapEntries(
            ConfigFieldDescriptor descriptor,
            Map<?, ?> map,
            ResourceLocationSemantics semantics,
            Object rawValue,
            ConfigRepairReport report
    )
    {
        Map<Object, Object> repairedMap = new LinkedHashMap<>();
        boolean repaired = false;

        for (Map.Entry<?, ?> entry : map.entrySet())
        {
            try
            {
                if (semantics.key() != ResourceLocationRegistry.NONE)
                {
                    requireExists(String.valueOf(entry.getKey()), semantics.key());
                }
                if (semantics.value() != ResourceLocationRegistry.NONE)
                {
                    requireExists(String.valueOf(entry.getValue()), semantics.value());
                }
                repairedMap.put(entry.getKey(), entry.getValue());
            }
            catch (ConfigValidationException e)
            {
                repaired = true;
                report.repaired(descriptor.path(), entry, "<removed>", e.getMessage());
            }
        }

        if (repaired)
        {
            report.repaired(descriptor.path(), rawValue, repairedMap, "Repaired table entries");
            return repairedMap;
        }
        return rawValue;
    }

    private static void requireExactPatternsExist(Object value, ResourceLocationRegistry registry) throws ConfigValidationException
    {
        if (!(value instanceof Iterable<?> patterns))
        {
            throw new ConfigValidationException("Expected list");
        }

        for (Object rawPattern : patterns)
        {
            requireExactPatternExists(rawPattern, registry);
        }
    }

    private static Object repairExactPatterns(
            ConfigFieldDescriptor descriptor,
            Object value,
            ResourceLocationRegistry registry,
            Object defaultValue,
            ConfigRepairReport report
    )
    {
        if (!(value instanceof Iterable<?> patterns))
        {
            Object fixedValue = descriptor.copyValue(defaultValue);
            report.repaired(descriptor.path(), value, fixedValue, "Expected list");
            return fixedValue;
        }

        List<Object> repairedPatterns = new ArrayList<>();
        boolean repaired = false;
        for (Object rawPattern : patterns)
        {
            try
            {
                requireExactPatternExists(rawPattern, registry);
                repairedPatterns.add(rawPattern);
            }
            catch (ConfigValidationException e)
            {
                repaired = true;
                report.repaired(descriptor.path(), rawPattern, "<removed>", e.getMessage());
            }
        }

        if (repaired)
        {
            report.repaired(descriptor.path(), value, repairedPatterns, "Repaired list entries");
            return repairedPatterns;
        }
        return value;
    }

    private static void requireExactPatternExists(Object rawPattern, ResourceLocationRegistry registry) throws ConfigValidationException
    {
        String pattern = String.valueOf(rawPattern);
        if (pattern.startsWith("!")) pattern = pattern.substring(1);
        if (pattern.contains("*")) return;
        requireExists(pattern, registry);
    }

    private static void requireExists(String rawId, ResourceLocationRegistry registry) throws ConfigValidationException
    {
        Identifier id = Identifier.tryParse(rawId);
        if (id == null || !exists(id, registry))
        {
            throw new ConfigValidationException("Unknown " + registry.name().toLowerCase(java.util.Locale.ROOT) + " id: " + rawId);
        }
    }

    private static boolean exists(Identifier id, ResourceLocationRegistry registry)
    {
        return switch (registry)
        {
            case NONE -> true;
            case BLOCK -> BuiltInRegistries.BLOCK.get(id).isPresent();
            case ENTITY -> BuiltInRegistries.ENTITY_TYPE.get(id).isPresent();
        };
    }
}
