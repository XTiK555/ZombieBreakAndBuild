package com.tik.zbb.config.runtime;

import com.tik.zbb.Constants;
import com.tik.zbb.config.ConfigDocument;
import com.tik.zbb.config.io.ConfigStorage;
import com.tik.zbb.config.io.ConfigStorageException;
import com.tik.zbb.config.schema.ConfigFileReport;

import java.util.Objects;

final class ConfigPersistence
{
    private final ConfigStorage storage;

    ConfigPersistence(ConfigStorage storage)
    {
        this.storage = Objects.requireNonNull(storage, "storage");
    }

    LoadResult load()
    {
        ConfigStorage.LoadedConfig loaded;
        try
        {
            loaded = storage.load();
        }
        catch (ConfigStorageException e)
        {
            return recover(e);
        }

        boolean fileChanged = loaded.fileReport().changed();
        if (fileChanged)
        {
            try
            {
                storage.save(loaded.document());
            }
            catch (ConfigStorageException e)
            {
                Constants.LOG.error("Failed to save normalized config", e);
                return LoadResult.failure(
                        loaded.fileReport(),
                        "Failed to save normalized config; previous in-memory config was kept"
                );
            }
        }

        return LoadResult.success(loaded.document(), fileChanged, loaded.fileReport(), "Reloaded config");
    }

    void save(ConfigDocument document) throws ConfigStorageException
    {
        storage.save(document);
    }

    private LoadResult recover(ConfigStorageException loadError)
    {
        Constants.LOG.error("Failed to parse config, attempting recovery before restoring defaults", loadError);
        ConfigDocument defaults = new ConfigDocument();
        try
        {
            ConfigStorage.RecoveryResult recovery = storage.recoverAfterLoadFailure(defaults);
            return LoadResult.success(defaults, recovery.fallbackSaved(), new ConfigFileReport(), recovery.message());
        }
        catch (ConfigStorageException recoveryError)
        {
            Constants.LOG.error("Failed to recover broken config; keeping previous in-memory config", recoveryError);
            return LoadResult.failure(
                    new ConfigFileReport(),
                    "Failed to load config; recovery failed and previous in-memory config was kept"
            );
        }
    }

    record LoadResult(boolean success, boolean saved, ConfigDocument document, ConfigFileReport fileReport, String message)
    {
        static LoadResult success(ConfigDocument document, boolean saved, ConfigFileReport report, String message)
        {
            return new LoadResult(true, saved, document, report, message);
        }

        static LoadResult failure(ConfigFileReport report, String message)
        {
            return new LoadResult(false, false, null, report, message);
        }
    }
}
