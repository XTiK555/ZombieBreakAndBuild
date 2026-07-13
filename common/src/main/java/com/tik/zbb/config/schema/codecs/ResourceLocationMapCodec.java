package com.tik.zbb.config.schema.codecs;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.tik.zbb.config.schema.ConfigFieldDescriptor;
import com.tik.zbb.config.schema.ConfigRepairReport;
import com.tik.zbb.config.schema.ConfigValidationException;
import com.tik.zbb.config.schema.ConfigValueCodec;
import com.tik.zbb.config.schema.ResourceLocationId;

import java.util.LinkedHashMap;
import java.util.Map;

abstract class ResourceLocationMapCodec implements ConfigValueCodec
{
    @Override
    public Object parseText(ConfigFieldDescriptor descriptor, String rawValue) throws ConfigValidationException
    {
        Map<String, Object> values = new LinkedHashMap<>();
        if (rawValue.isBlank()) return values;

        for (String entry : rawValue.split(","))
        {
            ResourceLocationMapEntry parsedEntry = parseEntryText(entry);
            values.put(parsedEntry.key(), parsedEntry.value());
        }
        return values;
    }

    @Override
    public Object decodeDocumentValue(ConfigFieldDescriptor descriptor, Object rawValue) throws ConfigValidationException
    {
        if (rawValue instanceof UnmodifiableConfig config)
        {
            return decodeConfigTable(config);
        }

        if (rawValue instanceof Map<?, ?> map)
        {
            return decodeMap(map);
        }

        throw new ConfigValidationException("Expected table");
    }

    @Override
    public Object repairDocumentValue(ConfigFieldDescriptor descriptor, Object rawValue, Object defaultValue, ConfigRepairReport report)
    {
        if (rawValue instanceof UnmodifiableConfig config)
        {
            return repairEntries(descriptor, config.entrySet(), rawValue, report);
        }

        if (rawValue instanceof Map<?, ?> map)
        {
            return repairEntries(descriptor, map.entrySet(), rawValue, report);
        }

        Object fixedValue = descriptor.copyValue(defaultValue);
        report.repaired(descriptor.path(), rawValue, fixedValue, "Expected table");
        return fixedValue;
    }

    @Override
    public Object normalizeValue(ConfigFieldDescriptor descriptor, Object value) throws ConfigValidationException
    {
        if (!(value instanceof Map<?, ?> map))
        {
            throw new ConfigValidationException("Expected table");
        }
        return decodeMap(map);
    }

    @Override
    public boolean supportsCollectionEdits()
    {
        return true;
    }

    @Override
    public Object emptyValue(ConfigFieldDescriptor descriptor)
    {
        return new LinkedHashMap<String, Object>();
    }

    @Override
    public Object parseEntry(ConfigFieldDescriptor descriptor, String rawValue) throws ConfigValidationException
    {
        return parseEntryText(rawValue);
    }

    @Override
    public Object addEntry(ConfigFieldDescriptor descriptor, Object value, Object entry) throws ConfigValidationException
    {
        Map<String, Object> values = copyMap(value);
        if (!(entry instanceof ResourceLocationMapEntry mapEntry))
        {
            throw new ConfigValidationException("Expected key=value");
        }
        values.put(mapEntry.key(), mapEntry.value());
        return values;
    }

    @Override
    public Object removeEntry(ConfigFieldDescriptor descriptor, Object value, Object entry) throws ConfigValidationException
    {
        Map<String, Object> values = copyMap(value);
        if (entry instanceof ResourceLocationMapEntry mapEntry)
        {
            values.remove(mapEntry.key());
            return values;
        }
        if (entry instanceof String key)
        {
            values.remove(normalizeIdentifier(key));
            return values;
        }
        throw new ConfigValidationException("Expected key or key=value");
    }

    protected abstract Object normalizeValue(Object rawValue) throws ConfigValidationException;

    protected abstract String valueError();

    private Map<String, Object> decodeConfigTable(UnmodifiableConfig config) throws ConfigValidationException
    {
        Map<String, Object> values = new LinkedHashMap<>();
        for (UnmodifiableConfig.Entry entry : config.entrySet())
        {
            values.put(normalizeIdentifier(entry.getKey()), normalizeValue(entry.getRawValue()));
        }
        return values;
    }

    private Map<String, Object> decodeMap(Map<?, ?> map) throws ConfigValidationException
    {
        Map<String, Object> values = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet())
        {
            if (!(entry.getKey() instanceof String key))
            {
                throw new ConfigValidationException("Expected resource location key");
            }
            values.put(normalizeIdentifier(key), normalizeValue(entry.getValue()));
        }
        return values;
    }

    private Map<String, Object> repairEntries(ConfigFieldDescriptor descriptor, Iterable<?> entries, Object rawValue, ConfigRepairReport report)
    {
        Map<String, Object> values = new LinkedHashMap<>();
        boolean repaired = false;

        for (Object entryObject : entries)
        {
            try
            {
                MapEntry entry = mapEntry(entryObject);
                String key = normalizeIdentifier(entry.key());
                Object value = normalizeValue(entry.value());
                values.put(key, value);
                if (!entry.key().equals(key) || !entry.value().equals(value)) repaired = true;
            }
            catch (ConfigValidationException e)
            {
                repaired = true;
                report.repaired(descriptor.path(), entryObject, "<removed>", e.getMessage());
            }
        }

        if (repaired)
        {
            report.repaired(descriptor.path(), rawValue, values, "Repaired table entries");
        }

        return values;
    }

    private ResourceLocationMapEntry parseEntryText(String rawValue) throws ConfigValidationException
    {
        String[] parts = rawValue.split("=", 2);
        if (parts.length != 2)
        {
            throw new ConfigValidationException("Expected key=value");
        }

        return new ResourceLocationMapEntry(normalizeIdentifier(parts[0]), normalizeValue(parts[1].trim()));
    }

    private static MapEntry mapEntry(Object entryObject) throws ConfigValidationException
    {
        if (entryObject instanceof UnmodifiableConfig.Entry entry)
        {
            return new MapEntry(entry.getKey(), entry.getRawValue());
        }

        if (entryObject instanceof Map.Entry<?, ?> entry && entry.getKey() instanceof String key)
        {
            return new MapEntry(key, entry.getValue());
        }

        throw new ConfigValidationException("Expected table entry");
    }

    private Map<String, Object> copyMap(Object value) throws ConfigValidationException
    {
        if (!(value instanceof Map<?, ?> map))
        {
            throw new ConfigValidationException("Expected table");
        }
        return decodeMap(map);
    }

    protected static String normalizeIdentifier(String rawValue) throws ConfigValidationException
    {
        return ResourceLocationId.normalize(rawValue);
    }

    private record MapEntry(String key, Object value) {}
}
