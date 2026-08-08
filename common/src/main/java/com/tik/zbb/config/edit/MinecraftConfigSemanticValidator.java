package com.tik.zbb.config.edit;

import com.tik.zbb.config.annotations.ResourceLocationRegistry;
import com.tik.zbb.config.annotations.ResourceLocationSemantics;
import com.tik.zbb.config.schema.ConfigFieldDescriptor;
import com.tik.zbb.config.schema.ConfigRepairReport;
import com.tik.zbb.config.schema.ConfigValidationException;
import com.tik.zbb.config.schema.codecs.ResourceLocationPatternListCodec;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MinecraftConfigSemanticValidator implements ConfigSemanticValidator
{
    private final RegistryAccess registries;

    public MinecraftConfigSemanticValidator(RegistryAccess registries)
    {
        this.registries = registries;
    }

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

    private void requireMapEntries(Map<?, ?> map, ResourceLocationSemantics semantics) throws ConfigValidationException
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

    private Object repairMapEntries(
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
                    requireValidId(String.valueOf(entry.getKey()));
                }
                if (semantics.value() != ResourceLocationRegistry.NONE)
                {
                    requireValidId(String.valueOf(entry.getValue()));
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

    private void requireExactPatternsExist(Object value, ResourceLocationRegistry registry) throws ConfigValidationException
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

    private Object repairExactPatterns(
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
                requireValidPattern(rawPattern, registry);
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

    private void requireExactPatternExists(Object rawPattern, ResourceLocationRegistry registry) throws ConfigValidationException
    {
        String pattern = String.valueOf(rawPattern);
        if (pattern.startsWith("!")) pattern = pattern.substring(1);
        if (pattern.startsWith("@"))
        {
            requireValidCategory(pattern, registry);
            return;
        }
        if (pattern.contains("*")) return;
        requireExists(pattern, registry);
    }

    private void requireValidPattern(Object rawPattern, ResourceLocationRegistry registry) throws ConfigValidationException
    {
        String pattern = String.valueOf(rawPattern);
        if (pattern.startsWith("!")) pattern = pattern.substring(1);
        if (pattern.startsWith("@"))
        {
            requireValidCategory(pattern, registry);
            return;
        }
        if (pattern.contains("*")) return;
        requireValidId(pattern);
    }

    private void requireValidCategory(String pattern, ResourceLocationRegistry registry) throws ConfigValidationException
    {
        if (registry != ResourceLocationRegistry.ENTITY)
        {
            throw new ConfigValidationException("Mob categories are only valid for entity lists");
        }
        ResourceLocationPatternListCodec.normalizePattern(pattern);
    }

    private void requireValidId(String rawId) throws ConfigValidationException
    {
        if (Identifier.tryParse(rawId) == null)
        {
            throw new ConfigValidationException("Invalid resource id: " + rawId);
        }
    }

    private void requireExists(String rawId, ResourceLocationRegistry registry) throws ConfigValidationException
    {
        Identifier id = Identifier.tryParse(rawId);
        if (id == null || !exists(id, registry))
        {
            throw new ConfigValidationException("Unknown " + registry.name().toLowerCase(java.util.Locale.ROOT) + " id: " + rawId);
        }
    }

    private boolean exists(Identifier id, ResourceLocationRegistry registry)
    {
        return switch (registry)
        {
            case NONE -> true;
            case BLOCK -> registries.lookupOrThrow(Registries.BLOCK).get(id).isPresent();
            case ENTITY -> registries.lookupOrThrow(Registries.ENTITY_TYPE).get(id).isPresent();
            case DIMENSION -> registries.lookupOrThrow(Registries.DIMENSION).get(id).isPresent();
        };
    }
}
