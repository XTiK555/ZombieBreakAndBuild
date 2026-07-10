package com.tik.zbb.config.io;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.serde.ObjectDeserializer;
import com.tik.zbb.config.ConfigData;
import com.tik.zbb.config.schema.*;

import java.util.ArrayList;
import java.util.List;

public final class ConfigDocumentNormalizer
{
    private static final Object MISSING = new Object();
    private final ObjectDeserializer deserializer;

    public ConfigDocumentNormalizer(ObjectDeserializer deserializer)
    {
        this.deserializer = deserializer;
    }

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

        ConfigData data = deserializer.deserializeFields(normalized, ConfigData::new);
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

    public record NormalizedConfig(ConfigData data, ConfigRepairReport repairReport) {}
}
