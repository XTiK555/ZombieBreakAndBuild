package com.tik.zbb.config.io;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.InMemoryCommentedFormat;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.serde.ObjectDeserializer;
import com.electronwill.nightconfig.toml.TomlWriter;
import com.tik.zbb.config.ConfigDocument;
import com.tik.zbb.config.io.format.ConfigComments;
import com.tik.zbb.config.io.format.ConfigFormatter;
import com.tik.zbb.config.schema.ConfigFieldDescriptor;
import com.tik.zbb.config.schema.ConfigPath;
import com.tik.zbb.config.schema.ConfigSchema;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;

public class ConfigFileStore implements ConfigStorage
{
    private final Path path;
    private final ConfigDocumentNormalizer documentNormalizer;
    private final CommentedFileConfig fileConfig;

    public ConfigFileStore(Path path)
    {
        this(path, new ConfigDocumentNormalizer(ObjectDeserializer.standard()));
    }

    public ConfigFileStore(Path path, ConfigDocumentNormalizer documentNormalizer)
    {
        this.path = path;
        this.documentNormalizer = documentNormalizer;
        this.fileConfig = CommentedFileConfig.builder(path)
                .preserveInsertionOrder()
                .sync()
                .build();
    }

    @Override
    public LoadedConfig load() throws ConfigPersistenceException
    {
        ConfigDocumentNormalizer.NormalizedConfig normalized = documentNormalizer.normalize(loadRaw());
        return new LoadedConfig(normalized.document(), normalized.repairReport());
    }

    private CommentedConfig loadRaw() throws ConfigPersistenceException
    {
        try
        {
            fileConfig.load();
            return fileConfig;
        }
        catch (Exception e)
        {
            throw new ConfigPersistenceException("Failed to load config", e);
        }
    }

    @Override
    public void save(ConfigDocument data) throws ConfigPersistenceException
    {
        Path tempPath = path.resolveSibling(path.getFileName() + ".tmp");
        try
        {
            CommentedConfig config = newOrderedConfig();
            writeInSchemaOrder(data, config);
            ConfigComments.apply(config, data);

            Path parent = path.getParent();
            if (parent != null)
            {
                Files.createDirectories(parent);
            }

            try (Writer writer = Files.newBufferedWriter(tempPath, StandardCharsets.UTF_8))
            {
                new TomlWriter().write(config, writer);
            }
            ConfigFormatter.format(tempPath);

            if (Files.exists(path))
            {
                Files.copy(path, backupPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            try
            {
                Files.move(tempPath, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            }
            catch (IOException | UnsupportedOperationException e)
            {
                Files.move(tempPath, path, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        catch (Exception e)
        {
            try
            {
                Files.deleteIfExists(tempPath);
            }
            catch (Exception suppressed)
            {
                e.addSuppressed(suppressed);
            }
            throw new ConfigPersistenceException("Failed to save config", e);
        }
    }

    @Override
    public RecoveryResult recoverAfterLoadFailure(ConfigDocument fallbackDocument) throws ConfigPersistenceException
    {
        Path brokenPath = moveBrokenFile();
        save(fallbackDocument);
        return new RecoveryResult(true, "Failed to load config; moved broken config to " + brokenPath + " and restored defaults");
    }

    private Path moveBrokenFile() throws ConfigPersistenceException
    {
        if (!Files.exists(path))
        {
            return path;
        }

        Path brokenPath = brokenPath();
        try
        {
            Files.move(path, brokenPath, StandardCopyOption.REPLACE_EXISTING);
            return brokenPath;
        }
        catch (Exception e)
        {
            throw new ConfigPersistenceException("Failed to preserve broken config", e);
        }
    }

    private Path backupPath()
    {
        return path.resolveSibling(path.getFileName() + ".bak");
    }

    private static void writeInSchemaOrder(ConfigDocument data, CommentedConfig config)
    {
        for (ConfigFieldDescriptor descriptor : ConfigSchema.descriptors())
        {
            setRaw(config, descriptor.path(), descriptor.getValue(data));
        }
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
                nested = newOrderedConfig();
                current.set(parts[i], nested);
            }
            current = nested;
        }

        current.set(parts[parts.length - 1], toConfigValue(value));
    }

    private static Object toConfigValue(Object value)
    {
        if (value instanceof java.util.Map<?, ?> map)
        {
            CommentedConfig config = newOrderedConfig();
            for (java.util.Map.Entry<?, ?> entry : map.entrySet())
            {
                String key = String.valueOf(entry.getKey());
                config.set(java.util.List.of(key), toConfigValue(entry.getValue()));
            }
            return config;
        }
        return value;
    }

    private static CommentedConfig newOrderedConfig()
    {
        return CommentedConfig.of(LinkedHashMap::new, InMemoryCommentedFormat.defaultInstance());
    }

    private Path brokenPath()
    {
        Path brokenPath = path.resolveSibling(path.getFileName() + ".broken");
        if (!Files.exists(brokenPath))
        {
            return brokenPath;
        }
        return path.resolveSibling(path.getFileName() + ".broken." + System.currentTimeMillis());
    }
}
