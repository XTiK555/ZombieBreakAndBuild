package com.tik.zbb.config;

import com.tik.zbb.config.document.ConfigDocumentCopier;

public final class ConfigSnapshot
{
    private final ConfigDocument document;
    private final ConfigGame game;
    private final long version;

    private ConfigSnapshot(ConfigDocument document, ConfigGame game, long version)
    {
        this.document = document;
        this.game = game;
        this.version = version;
    }

    public static ConfigSnapshot create(ConfigDocument document, long version)
    {
        return create(document, version, ConfigGame.BlockResolver.NONE);
    }

    public static ConfigSnapshot create(ConfigDocument document, long version, ConfigGame.BlockResolver blockResolver)
    {
        ConfigDocument documentData = ConfigDocumentCopier.copy(document);
        return new ConfigSnapshot(documentData, ConfigGame.create(documentData, blockResolver), version);
    }

    public ConfigDocument document()
    {
        return ConfigDocumentCopier.copy(document);
    }

    public ConfigGame game()
    {
        return game;
    }

    public long version()
    {
        return version;
    }
}
