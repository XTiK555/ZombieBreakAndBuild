package com.tik.zbb.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.serde.ObjectDeserializer;
import com.electronwill.nightconfig.core.serde.ObjectSerializer;
import com.tik.zbb.Constants;
import com.tik.zbb.config.tools.ConfigComments;
import com.tik.zbb.config.tools.ConfigFormatter;
import com.tik.zbb.config.tools.ConfigSanitizer;
import com.tik.zbb.platform.Services;

import java.nio.file.Path;

public final class ConfigManager
{
    private static final ObjectSerializer SERIALIZER = ObjectSerializer.standard();
    private static final ObjectDeserializer DESERIALIZER = ObjectDeserializer.standard();

    private static volatile CommentedFileConfig FILE_CONFIG;
    private static volatile ConfigData DATA = new ConfigData();
    private static volatile ConfigSnapshot SNAPSHOT = ConfigSnapshot.create(DATA, 0);
    private static volatile long VERSION = 0;

    public static void init()
    {
        Config.setInsertionOrderPreserved(true);

        String modName = Constants.MOD_NAME.replaceAll("\\s", "-").toLowerCase();
        Path configPath = Services.PLATFORM.getConfigDir().resolve(modName + ".toml");

        FILE_CONFIG = CommentedFileConfig.builder(configPath).sync().build();

        reload();
    }

    public static ConfigSnapshot getConfigSnapshot()
    {
        return SNAPSHOT;
    }

    public static synchronized void reload()
    {
        if (FILE_CONFIG == null)
        {
            Constants.LOG.warn("Config reload requested before init");
            return;
        }

        ConfigData defaults = new ConfigData();

        try
        {
            FILE_CONFIG.load();
        }
        catch (Exception e)
        {
            Constants.LOG.error("Failed to parse config, restoring defaults", e);
            setData(defaults);
            save();
            return;
        }

        try
        {
            ConfigSanitizer.sanitize(FILE_CONFIG, defaults, SERIALIZER);

            ConfigData loaded = DESERIALIZER.deserializeFields(FILE_CONFIG, ConfigData::new);

            setData(loaded);
            save();

            Constants.LOG.info("Reloaded config");
        }
        catch (Exception e)
        {
            Constants.LOG.error("Failed to sanitize/deserialize config, restoring defaults", e);
            setData(defaults);
            save();
        }
    }

    private static void save()
    {
        try
        {
            CommentedConfig config = CommentedConfig.inMemory();

            SERIALIZER.serializeFields(DATA, config);
            ConfigComments.apply(config, DATA);

            FILE_CONFIG.clear();
            FILE_CONFIG.putAll(config);
            FILE_CONFIG.save();
        }
        catch (Exception e)
        {
            Constants.LOG.error("Failed to save config", e);
            return;
        }

        try
        {
            ConfigFormatter.addHeader(FILE_CONFIG.getNioPath());
            ConfigFormatter.splitBlocks(FILE_CONFIG.getNioPath());
        }
        catch (Exception e)
        {
            Constants.LOG.warn("Failed to format config file", e);
        }
    }

    private static void setData(ConfigData data)
    {
        DATA = data;
        VERSION++;
        SNAPSHOT = ConfigSnapshot.create(DATA, VERSION);
    }
}