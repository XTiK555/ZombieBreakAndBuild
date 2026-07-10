package com.tik.zbb.config.io;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.tik.zbb.config.ConfigData;
import com.tik.zbb.config.schema.*;
import com.tik.zbb.utilities.ConfigUtilities;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public final class ConfigDocumentNormalizer
{
    private static final Object MISSING = new Object();

    public NormalizedConfig normalize(UnmodifiableConfig rawConfig)
    {
        ConfigRepairReport report = new ConfigRepairReport();
        CommentedConfig normalized = CommentedConfig.inMemory();

        for (ConfigFieldDescriptor descriptor : ConfigSchema.descriptors())
        {
            Object defaultValue = descriptor.defaultValue();
            Object rawValue = getRaw(rawConfig, descriptor.path());
            Object value;

            if (rawValue == MISSING)
            {
                report.repaired(descriptor.path(), "<missing>", defaultValue, "Missing config value");
                value = descriptor.copyValue(defaultValue);
            }
            else
            {
                value = repairOrDecode(descriptor, rawValue, defaultValue, report);
            }

            setRaw(normalized, descriptor.path(), value);
        }

        ConfigData data = readObject(normalized, ConfigData.class);
        return new NormalizedConfig(data, report);
    }

    private static Object repairOrDecode(ConfigFieldDescriptor descriptor, Object rawValue, Object defaultValue, ConfigRepairReport report)
    {
        if (descriptor.kind() == ConfigValueKind.STRING_LIST)
        {
            return repairOrDecodeList(descriptor, rawValue, defaultValue, report);
        }

        try
        {
            return descriptor.codec().decodeDocumentValue(descriptor, rawValue);
        }
        catch (ConfigValidationException e)
        {
            Object fixedValue = descriptor.copyValue(defaultValue);
            report.repaired(descriptor.path(), rawValue, fixedValue, e.getMessage());
            return fixedValue;
        }
    }

    private static Object repairOrDecodeList(ConfigFieldDescriptor descriptor, Object rawValue, Object defaultValue, ConfigRepairReport report)
    {
        if (!(rawValue instanceof List<?> list))
        {
            Object fixedValue = descriptor.copyValue(defaultValue);
            report.repaired(descriptor.path(), rawValue, fixedValue, "Expected list");
            return fixedValue;
        }

        List<String> cleaned = new ArrayList<>();
        boolean repaired = false;
        for (Object entry : list)
        {
            try
            {
                @SuppressWarnings("unchecked")
                List<String> decodedEntry = (List<String>) descriptor.codec().decodeDocumentValue(descriptor, List.of(entry));
                cleaned.addAll(decodedEntry);
                if (!entry.equals(decodedEntry.get(0)))
                {
                    repaired = true;
                }
            }
            catch (ConfigValidationException e)
            {
                repaired = true;
                report.repaired(descriptor.path(), entry, "<removed>", e.getMessage());
            }
        }

        if (repaired)
        {
            report.repaired(descriptor.path(), rawValue, cleaned, "Repaired list entries");
        }

        return cleaned;
    }

    private static Object getRaw(UnmodifiableConfig root, ConfigPath path)
    {
        String[] parts = path.value().split("\\.");
        UnmodifiableConfig current = root;

        for (int i = 0; i < parts.length - 1; i++)
        {
            Object next = current.getRaw(parts[i]);
            if (!(next instanceof UnmodifiableConfig nested))
            {
                return MISSING;
            }
            current = nested;
        }

        Object value = current.getRaw(parts[parts.length - 1]);
        return value == null ? MISSING : value;
    }

    private static void setRaw(CommentedConfig root, ConfigPath path, Object value)
    {
        String[] parts = path.value().split("\\.");
        CommentedConfig current = root;

        for (int i = 0; i < parts.length - 1; i++)
        {
            Object next = current.getRaw(parts[i]);
            CommentedConfig nested;
            if (next instanceof CommentedConfig existingNested)
            {
                nested = existingNested;
            }
            else
            {
                nested = CommentedConfig.inMemory();
                current.set(parts[i], nested);
            }
            current = nested;
        }

        current.set(parts[parts.length - 1], value);
    }

    private static <T> T readObject(CommentedConfig config, Class<T> type)
    {
        try
        {
            T instance = type.getDeclaredConstructor().newInstance();

            for (Field field : ConfigUtilities.getConfigFields(type))
            {
                String key = field.getName();

                if (ConfigUtilities.isNestedConfigField(field))
                {
                    Object rawNested = config.getRaw(key);
                    if (rawNested instanceof CommentedConfig nestedConfig)
                    {
                        field.set(instance, readObject(nestedConfig, field.getType()));
                    }
                    continue;
                }

                Object converted = convertLoadedValue(config.getRaw(key), field.getType());
                if (converted != null)
                {
                    field.set(instance, converted);
                }
            }

            return instance;
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to read config object: " + type.getName(), e);
        }
    }

    private static Object convertLoadedValue(Object value, Class<?> targetType)
    {
        if (value == null) return null;
        if (targetType == boolean.class || targetType == Boolean.class) return value instanceof Boolean b ? b : null;
        if (targetType == int.class || targetType == Integer.class) return value instanceof Number n ? n.intValue() : null;
        if (targetType == long.class || targetType == Long.class) return value instanceof Number n ? n.longValue() : null;
        if (targetType == double.class || targetType == Double.class) return value instanceof Number n ? n.doubleValue() : null;
        if (targetType == float.class || targetType == Float.class) return value instanceof Number n ? n.floatValue() : null;
        if (targetType == String.class) return value instanceof String s ? s : null;

        if (List.class.isAssignableFrom(targetType))
        {
            if (!(value instanceof List<?> list)) return null;

            List<String> result = new ArrayList<>();
            for (Object element : list)
            {
                if (element instanceof String s)
                {
                    result.add(s);
                }
            }
            return result;
        }

        return value;
    }

    public record NormalizedConfig(ConfigData data, ConfigRepairReport repairReport) {}
}
