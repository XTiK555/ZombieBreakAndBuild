package com.tik.zbb.config.io;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.serde.ObjectDeserializer;
import com.tik.zbb.config.ConfigDocument;
import com.tik.zbb.config.schema.ConfigFieldDescriptor;
import com.tik.zbb.config.schema.ConfigPath;
import com.tik.zbb.config.schema.ConfigRepairReport;
import com.tik.zbb.config.schema.ConfigSchema;

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
                value = descriptor.codec().repairDocumentValue(descriptor, rawValue, defaultValue, report);
            }

            setRaw(normalized, descriptor.path(), value);
        }

        ConfigDocument document = deserializer.deserializeFields(normalized, ConfigDocument::new);
        return new NormalizedConfig(document, report);
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

    public record NormalizedConfig(ConfigDocument document, ConfigRepairReport repairReport) {}
}
