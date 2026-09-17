package com.tik.zbb.config;

import com.tik.zbb.utilities.ConfigUtilities;

public final class ConfigSnapshot
{
    private final ConfigDocument document;
    private final ConfigRuntime game;
    private final long version;

    private ConfigSnapshot(ConfigDocument document, ConfigRuntime game, long version)
    {
        this.document = document;
        this.game = game;
        this.version = version;
    }

    public static ConfigSnapshot create(ConfigDocument document, long version, ConfigRuntime.BlockResolver blockResolver)
    {
        ConfigDocument documentData = ConfigUtilities.copyConfig(document);
        return new ConfigSnapshot(documentData, ConfigRuntime.create(documentData, blockResolver), version);
    }

    public ConfigDocument document()
    {
        return ConfigUtilities.copyConfig(document);
    }

    public ConfigRuntime game()
    {
        return game;
    }

    public long version()
    {
        return version;
    }
}
