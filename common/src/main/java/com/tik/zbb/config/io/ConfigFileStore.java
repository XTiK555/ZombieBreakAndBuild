package com.tik.zbb.config.io;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.serde.ObjectSerializer;
import com.tik.zbb.Constants;
import com.tik.zbb.config.ConfigData;
import com.tik.zbb.config.tools.ConfigComments;
import com.tik.zbb.config.tools.ConfigFormatter;

import java.nio.file.Path;

public class ConfigFileStore
{
    private final ObjectSerializer serializer;
    private final CommentedFileConfig fileConfig;

    public ConfigFileStore(Path path, ObjectSerializer serializer)
    {
        this.serializer = serializer;
        this.fileConfig = CommentedFileConfig.builder(path).sync().build();
    }

    public CommentedConfig loadRaw() throws ConfigPersistenceException
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

    public void save(ConfigData data) throws ConfigPersistenceException
    {
        try
        {
            CommentedConfig config = CommentedConfig.inMemory();
            serializer.serializeFields(data, config);
            ConfigComments.apply(config, data);

            fileConfig.clear();
            fileConfig.putAll(config);
            fileConfig.save();
        }
        catch (Exception e)
        {
            throw new ConfigPersistenceException("Failed to save config", e);
        }

        try
        {
            ConfigFormatter.format(fileConfig.getNioPath());
        }
        catch (Exception e)
        {
            Constants.LOG.warn("Failed to format config file", e);
        }
    }
}
