package com.tik.zbb.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.serde.ObjectDeserializer;
import com.electronwill.nightconfig.core.serde.ObjectSerializer;
import com.tik.zbb.Constants;
import com.tik.zbb.platform.Services;

import java.nio.file.Path;

public final class ConfigManager
{
    private static final ObjectSerializer SERIALIZER = ObjectSerializer.standard();
    private static final ObjectDeserializer DESERIALIZER = ObjectDeserializer.standard();

    private static volatile CommentedFileConfig FILE_CONFIG;
    private static volatile ConfigData DATA = new ConfigData();
    private static volatile long VERSION = 0;

    public static void init()
    {
        Config.setInsertionOrderPreserved(true);

        String modName = Constants.MOD_NAME.replaceAll("\\s", "-").toLowerCase();
        Path configPath = Services.PLATFORM.getConfigDir().resolve(modName + ".toml");

        FILE_CONFIG = CommentedFileConfig.builder(configPath).sync().build();
        FILE_CONFIG.load();

        reload();
        save();
    }

    public static ConfigSnapshot getConfigSnapshot()
    {
        return new ConfigSnapshot(DATA, VERSION);
    }

    public static void reload()
    {
        ConfigData configData = FILE_CONFIG.isEmpty()
                ? new ConfigData()
                : DESERIALIZER.deserializeFields(FILE_CONFIG, ConfigData::new);
        configData.rebuildSets();

        DATA = configData;
        VERSION++;

        Constants.LOG.info("Reloading Config");
    }

    private static void save()
    {
        CommentedConfig config = CommentedConfig.inMemory();

        SERIALIZER.serializeFields(DATA, config);
        ConfigComments.apply(config, DATA);

        FILE_CONFIG.clear();
        FILE_CONFIG.putAll(config);
        FILE_CONFIG.save();

        ConfigFormatter.splitBlocks(FILE_CONFIG.getNioPath());
    }
}
