package com.tik.zbb.config.edit;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.tik.zbb.Constants;
import com.tik.zbb.config.ConfigData;
import com.tik.zbb.config.ConfigSnapshot;
import com.tik.zbb.config.edit.handler.*;
import com.tik.zbb.config.io.ConfigDocumentNormalizer;
import com.tik.zbb.config.io.ConfigFileStore;
import com.tik.zbb.config.io.ConfigPersistenceException;
import com.tik.zbb.config.runtime.ConfigRepository;
import com.tik.zbb.config.schema.ConfigFieldDescriptor;
import com.tik.zbb.config.schema.ConfigRepairReport;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class ConfigEditService
{
    private final ConfigRepository repository;
    private final ConfigFileStore fileStore;
    private final ConfigDocumentNormalizer documentNormalizer;
    private final ConfigEditContext editContext;
    private final Map<ConfigEditOperation, ConfigEditHandler> handlers;

    public ConfigEditService(ConfigRepository repository, ConfigFileStore fileStore, ConfigDocumentNormalizer documentNormalizer)
    {
        this.repository = repository;
        this.fileStore = fileStore;
        this.documentNormalizer = documentNormalizer;
        this.editContext = new ConfigEditContext(repository, fileStore);
        this.handlers = buildHandlers(List.of(
                new SetConfigEditHandler(),
                new AddConfigEditHandler(),
                new RemoveConfigEditHandler(),
                new ClearConfigEditHandler(),
                new ResetConfigEditHandler(),
                new ResetAllConfigEditHandler(),
                new DiscardConfigEditHandler(),
                new DiscardAllConfigEditHandler()
        ));
    }

    public ConfigSnapshot snapshot()
    {
        return repository.snapshot();
    }

    public Object effectiveValue(ConfigFieldDescriptor descriptor)
    {
        return repository.effectiveValue(descriptor);
    }

    public ConfigReloadResult reloadFromFile()
    {
        CommentedConfig rawConfig;
        try
        {
            rawConfig = fileStore.loadRaw();
        }
        catch (ConfigPersistenceException e)
        {
            Constants.LOG.error("Failed to parse config, restoring defaults", e);
            ConfigData defaults = new ConfigData();
            repository.replacePersisted(defaults);
            trySaveDefaults(defaults);
            return new ConfigReloadResult(false, null, "Failed to load config; restored defaults");
        }

        ConfigDocumentNormalizer.NormalizedConfig normalized = documentNormalizer.normalize(rawConfig);
        repository.replacePersisted(normalized.data());

        try
        {
            fileStore.save(normalized.data());
        }
        catch (ConfigPersistenceException e)
        {
            Constants.LOG.error("Failed to save normalized config", e);
            return new ConfigReloadResult(false, normalized.repairReport(), "Reloaded config, but failed to save normalized file");
        }

        return new ConfigReloadResult(true, normalized.repairReport(), "Reloaded config");
    }

    public ConfigEditResult edit(ConfigEditRequest request)
    {
        ConfigEditHandler handler = handlers.get(request.operation());
        if (handler == null)
        {
            return ConfigEditResult.failure(request, "Unsupported config edit operation: " + request.operation());
        }

        return handler.handle(request, editContext);
    }

    private static Map<ConfigEditOperation, ConfigEditHandler> buildHandlers(List<ConfigEditHandler> handlers)
    {
        Map<ConfigEditOperation, ConfigEditHandler> byOperation = new EnumMap<>(ConfigEditOperation.class);
        for (ConfigEditHandler handler : handlers)
        {
            ConfigEditHandler previous = byOperation.put(handler.operation(), handler);
            if (previous != null)
            {
                throw new IllegalStateException("Duplicate config edit handler for " + handler.operation());
            }
        }
        return Map.copyOf(byOperation);
    }

    private void trySaveDefaults(ConfigData defaults)
    {
        try
        {
            fileStore.save(defaults);
        }
        catch (ConfigPersistenceException e)
        {
            Constants.LOG.error("Failed to save default config", e);
        }
    }

    public record ConfigReloadResult(boolean saved, ConfigRepairReport repairReport, String message) {}
}
