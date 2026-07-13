package com.tik.zbb.config.io;

import com.tik.zbb.config.ConfigDocument;
import com.tik.zbb.config.schema.ConfigRepairReport;

public interface ConfigStorage
{
    LoadedConfig load() throws ConfigStorageException;

    void save(ConfigDocument document) throws ConfigStorageException;

    RecoveryResult recoverAfterLoadFailure(ConfigDocument fallbackDocument) throws ConfigStorageException;

    record LoadedConfig(ConfigDocument document, ConfigRepairReport repairReport) {}

    record RecoveryResult(boolean fallbackSaved, String message) {}
}
