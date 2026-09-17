package com.tik.zbb.config.edit;

import com.tik.zbb.config.annotations.ResourceLocationRegistry;
import com.tik.zbb.config.annotations.ResourceLocationSemantics;
import com.tik.zbb.config.runtime.ConfigAvailabilityReport;
import com.tik.zbb.config.schema.ConfigFieldDescriptor;
import com.tik.zbb.config.schema.ConfigValidationException;
import com.tik.zbb.config.schema.codecs.ResourceLocationPatternListCodec;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;

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
    public Object resolveValue(ConfigFieldDescriptor descriptor, Object value, Object defaultValue, ConfigAvailabilityReport report)
    {
        ResourceLocationSemantics semantics = descriptor.resourceLocationSemantics();
        if (semantics == null) return value;

        if (semantics.element() != ResourceLocationRegistry.NONE)
        {
            return resolveExactPatterns(descriptor, value, semantics.element(), defaultValue, report);
        }

        if (value instanceof Map<?, ?> map)
        {
            return resolveMapEntries(descriptor, map, semantics, value, report);
        }

        return ConfigSemanticValidator.super.resolveValue(descriptor, value, defaultValue, report);
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

    private Object resolveMapEntries(ConfigFieldDescriptor descriptor, Map<?, ?> map, ResourceLocationSemantics semantics, Object rawValue,
                                     ConfigAvailabilityReport report)
    {
        Map<Object, Object> availableEntries = new LinkedHashMap<>();
        boolean unavailableEntryFound = false;

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
                availableEntries.put(entry.getKey(), entry.getValue());
            }
            catch (ConfigValidationException e)
            {
                unavailableEntryFound = true;
                report.unavailable(descriptor.path(), entry, e.getMessage());
            }
        }

        if (unavailableEntryFound)
        {
            return availableEntries;
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

    private Object resolveExactPatterns(ConfigFieldDescriptor descriptor, Object value, ResourceLocationRegistry registry, Object defaultValue,
                                        ConfigAvailabilityReport report)
    {
        if (!(value instanceof Iterable<?> patterns))
        {
            Object fixedValue = descriptor.copyValue(defaultValue);
            report.unavailable(descriptor.path(), value, "Expected list");
            return fixedValue;
        }

        List<Object> availablePatterns = new ArrayList<>();
        boolean unavailablePatternFound = false;
        for (Object rawPattern : patterns)
        {
            try
            {
                requireExactPatternExists(rawPattern, registry);
                availablePatterns.add(rawPattern);
            }
            catch (ConfigValidationException e)
            {
                unavailablePatternFound = true;
                report.unavailable(descriptor.path(), rawPattern, e.getMessage());
            }
        }

        if (unavailablePatternFound)
        {
            return availablePatterns;
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

    private void requireValidCategory(String pattern, ResourceLocationRegistry registry) throws ConfigValidationException
    {
        if (registry != ResourceLocationRegistry.ENTITY)
        {
            throw new ConfigValidationException("Mob categories are only valid for entity lists");
        }
        ResourceLocationPatternListCodec.normalizePattern(pattern);
    }

    private void requireExists(String rawId, ResourceLocationRegistry registry) throws ConfigValidationException
    {
        ResourceLocation id = ResourceLocation.tryParse(rawId);
        if (id == null || !exists(id, registry))
        {
            throw new ConfigValidationException("Unknown " + registry.name().toLowerCase(java.util.Locale.ROOT) + " id: " + rawId);
        }
    }

    private boolean exists(ResourceLocation id, ResourceLocationRegistry registry)
    {
        return switch (registry)
        {
            case NONE -> true;
            case BLOCK -> registries.registryOrThrow(Registries.BLOCK).containsKey(id);
            case ENTITY -> registries.registryOrThrow(Registries.ENTITY_TYPE).containsKey(id);
            case DIMENSION -> registries.registryOrThrow(Registries.DIMENSION).containsKey(id);
        };
    }
}
