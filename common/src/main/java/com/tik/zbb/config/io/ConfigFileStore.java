package com.tik.zbb.config.io;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.tik.zbb.Constants;
import com.tik.zbb.config.ConfigData;
import com.tik.zbb.config.tools.ConfigComments;
import com.tik.zbb.config.tools.ConfigFormatter;
import com.tik.zbb.utilities.ConfigUtilities;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ConfigFileStore
{
    private final CommentedFileConfig fileConfig;

    public ConfigFileStore(Path path)
    {
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
            writeObject(data, config);
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

    private static void writeObject(Object source, CommentedConfig target)
    {
        try
        {
            for (Field field : ConfigUtilities.getConfigFields(source.getClass()))
            {
                String key = field.getName();
                Object value = field.get(source);

                if (value == null) continue;

                if (ConfigUtilities.isNestedConfigField(field))
                {
                    CommentedConfig nested = CommentedConfig.inMemory();
                    writeObject(value, nested);
                    target.set(key, nested);
                    continue;
                }

                if (value instanceof List<?> list)
                {
                    target.set(key, new ArrayList<>(list));
                }
                else
                {
                    target.set(key, value);
                }
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to write config object: " + source.getClass().getName(), e);
        }
    }
}
