package com.tik.zbb.config.edit;

import com.tik.zbb.Constants;
import com.tik.zbb.config.ConfigDocument;
import com.tik.zbb.config.ConfigSnapshot;
import com.tik.zbb.config.io.ConfigStorage;
import com.tik.zbb.config.io.ConfigStorageException;
import com.tik.zbb.config.runtime.ConfigRepository;
import com.tik.zbb.config.schema.*;

import java.util.Objects;

public final class ConfigEditService
{
    private final ConfigRepository repository;
    private final ConfigStorage storage;
    private final ConfigSemanticValidator semanticValidator;

    public ConfigEditService(ConfigRepository repository, ConfigStorage storage)
    {
        this(repository, storage, ConfigSemanticValidator.NONE);
    }

    public ConfigEditService(ConfigRepository repository, ConfigStorage storage, ConfigSemanticValidator semanticValidator)
    {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.storage = Objects.requireNonNull(storage, "storage");
        this.semanticValidator = Objects.requireNonNull(semanticValidator, "semanticValidator");
    }

    public ConfigSnapshot snapshot()
    {
        return repository.snapshot();
    }

    public Object effectiveValue(ConfigFieldDescriptor descriptor)
    {
        return repository.effectiveValue(descriptor);
    }

    /**
     * Loads the initial runtime state without consulting external game registries.
     * File-format normalization and recovery still run, but registry-backed semantic
     * validation is deferred until a normal reload after mod registration completes.
     */
    public synchronized ConfigReloadResult bootstrapFromFile()
    {
        return loadFromFile(ConfigSemanticValidator.NONE);
    }

    public synchronized ConfigReloadResult reloadFromFile()
    {
        return loadFromFile(semanticValidator);
    }

    private ConfigReloadResult loadFromFile(ConfigSemanticValidator reloadValidator)
    {
        ConfigStorage.LoadedConfig loaded;
        try
        {
            loaded = storage.load();
        }
        catch (ConfigStorageException e)
        {
            Constants.LOG.error("Failed to parse config, attempting recovery before restoring defaults", e);
            ConfigDocument defaults = new ConfigDocument();
            try
            {
                ConfigStorage.RecoveryResult recovery = storage.recoverAfterLoadFailure(defaults);
                repository.replacePersisted(defaults);
                return new ConfigReloadResult(true, recovery.fallbackSaved(), null, recovery.message());
            }
            catch (ConfigStorageException recoveryError)
            {
                Constants.LOG.error("Failed to recover broken config; keeping previous in-memory config", recoveryError);
                return new ConfigReloadResult(false, false, null, "Failed to load config; recovery failed and previous in-memory config was kept");
            }
        }

        boolean semanticValuesRepaired = repairSemanticValues(
                loaded.document(),
                loaded.repairReport(),
                reloadValidator
        );
        if (!semanticValuesRepaired && !loaded.repairReport().hasEntries())
        {
            repository.replacePersisted(loaded.document());
            return new ConfigReloadResult(true, false, loaded.repairReport(), "Reloaded config");
        }

        try
        {
            storage.save(loaded.document());
        }
        catch (ConfigStorageException e)
        {
            Constants.LOG.error("Failed to save normalized config", e);
            return new ConfigReloadResult(false, false, loaded.repairReport(), "Failed to save normalized config; previous in-memory config was kept");
        }

        repository.replacePersisted(loaded.document());
        return new ConfigReloadResult(true, true, loaded.repairReport(), "Reloaded config");
    }

    public synchronized ConfigEditResult edit(ConfigEditRequest request)
    {
        return switch (request.operation())
        {
            case SET -> set(request);
            case ADD -> add(request);
            case REMOVE -> remove(request);
            case CLEAR -> clear(request);
            case RESET_TO_DEFAULT -> reset(request);
            case RESET_ALL_TO_DEFAULTS -> resetAll(request);
            case REVERT_TO_PERSISTED -> discard(request);
            case DISCARD_ALL_OVERRIDES -> discardAll(request);
        };
    }

    public synchronized ConfigEditResult editRaw(ConfigEditRequest request)
    {
        if (request.operation() == ConfigEditOperation.RESET_ALL_TO_DEFAULTS
                || request.operation() == ConfigEditOperation.DISCARD_ALL_OVERRIDES)
        {
            return edit(request);
        }

        ConfigFieldDescriptor descriptor = findDescriptor(request);
        if (descriptor == null)
        {
            return ConfigEditResult.failure(request, "Unknown config path: " + request.path());
        }

        try
        {
            ConfigEditRequest typedRequest = switch (request.operation())
            {
                case SET ->
                        request.withValue(descriptor.codec().parseText(descriptor, String.valueOf(request.value())));
                case ADD -> request.withValue(externalEntry(descriptor, request.value()));
                case REMOVE -> request.withValue(externalRemovalEntry(descriptor, request.value()));
                case CLEAR, RESET_TO_DEFAULT, RESET_ALL_TO_DEFAULTS, REVERT_TO_PERSISTED, DISCARD_ALL_OVERRIDES ->
                        request;
            };
            return edit(typedRequest);
        }
        catch (ConfigValidationException e)
        {
            return ConfigEditResult.failure(request, descriptor.path() + ": " + e.getMessage());
        }
    }

    private boolean repairSemanticValues(ConfigDocument document, ConfigRepairReport report, ConfigSemanticValidator reloadValidator)
    {
        boolean repaired = false;
        for (ConfigFieldDescriptor descriptor : ConfigSchema.descriptors())
        {
            Object value = descriptor.getValue(document);
            Object repairedValue = reloadValidator.repairValue(descriptor, value, descriptor.defaultValue(), report);
            if (!Objects.equals(value, repairedValue))
            {
                descriptor.setValue(document, repairedValue);
                repaired = true;
            }
        }
        return repaired;
    }

    private ConfigEditResult set(ConfigEditRequest request)
    {
        ConfigFieldDescriptor descriptor = findDescriptor(request);
        if (descriptor == null) return ConfigEditResult.failure(request, "Unknown config path: " + request.path());

        return applyValue(request, descriptor, (baseData, currentDescriptor) -> request.value());
    }

    private ConfigEditResult add(ConfigEditRequest request)
    {
        ConfigFieldDescriptor descriptor = findListDescriptor(request);
        if (descriptor == null) return ConfigEditResult.failure(request, request.path() + " is not a collection");

        return applyValue(request, descriptor, (baseData, currentDescriptor) ->
                currentDescriptor.codec().addEntry(currentDescriptor, currentDescriptor.getValue(baseData), request.value()));
    }

    private ConfigEditResult remove(ConfigEditRequest request)
    {
        ConfigFieldDescriptor descriptor = findListDescriptor(request);
        if (descriptor == null) return ConfigEditResult.failure(request, request.path() + " is not a collection");

        return applyValue(request, descriptor, (baseData, currentDescriptor) ->
                currentDescriptor.codec().removeEntry(currentDescriptor, currentDescriptor.getValue(baseData), request.value()));
    }

    private ConfigEditResult clear(ConfigEditRequest request)
    {
        ConfigFieldDescriptor descriptor = findListDescriptor(request);
        if (descriptor == null) return ConfigEditResult.failure(request, request.path() + " is not a collection");

        return applyValue(request, descriptor, (baseData, currentDescriptor) ->
                currentDescriptor.codec().emptyValue(currentDescriptor));
    }

    private ConfigEditResult reset(ConfigEditRequest request)
    {
        ConfigFieldDescriptor descriptor = findDescriptor(request);
        if (descriptor == null) return ConfigEditResult.failure(request, "Unknown config path: " + request.path());

        return applyValue(request, descriptor, (baseData, currentDescriptor) -> currentDescriptor.defaultValue());
    }

    private ConfigEditResult resetAll(ConfigEditRequest request)
    {
        if (request.writeMode() == ConfigWriteMode.PERSISTENT)
        {
            ConfigDocument defaults = new ConfigDocument();
            try
            {
                storage.save(defaults);
                repository.replacePersisted(defaults);
            }
            catch (ConfigStorageException e)
            {
                Constants.LOG.error("Failed to save config", e);
                return ConfigEditResult.failure(request, "Failed to save config: " + e.getMessage());
            }

            return ConfigEditResult.success(request, null, true, ConfigSchema.descriptors().size(), "reset all");
        }

        int count = repository.resetRuntimeOverrides();
        return ConfigEditResult.success(request, null, false, count, "reset all temporary value(s)");
    }

    private ConfigEditResult discard(ConfigEditRequest request)
    {
        if (!ConfigSchema.hasPathOrSection(request.path()))
        {
            return ConfigEditResult.failure(request, "Unknown config path or section: " + request.path());
        }

        int count = repository.discard(request.path());
        return ConfigEditResult.success(request, null, false, count, "discarded " + count + " temporary value(s)");
    }

    private ConfigEditResult discardAll(ConfigEditRequest request)
    {
        int count = repository.discardAll();
        return ConfigEditResult.success(request, null, false, count, "discarded " + count + " temporary value(s)");
    }

    private ConfigEditResult applyValue(
            ConfigEditRequest request,
            ConfigFieldDescriptor descriptor,
            ConfigValueMutation mutation
    )
    {
        try
        {
            Object effectiveValue = request.writeMode() == ConfigWriteMode.PERSISTENT
                    ? updatePersistent(descriptor, mutation)
                    : updateRuntime(descriptor, mutation);
            return ConfigEditResult.success(
                    request,
                    effectiveValue,
                    request.writeMode() == ConfigWriteMode.PERSISTENT,
                    1,
                    "updated"
            );
        }
        catch (ConfigValidationException e)
        {
            return ConfigEditResult.failure(request, descriptor.path() + ": " + e.getMessage());
        }
        catch (ConfigStorageException e)
        {
            Constants.LOG.error("Failed to save config", e);
            return ConfigEditResult.failure(request, "Failed to save config: " + e.getMessage());
        }
    }

    private Object updatePersistent(ConfigFieldDescriptor descriptor, ConfigValueMutation mutation)
            throws ConfigValidationException, ConfigStorageException
    {
        ConfigDocument newPersisted = repository.persistedDocument();
        Object value = validateValue(descriptor, mutation.apply(newPersisted, descriptor));
        descriptor.setValue(newPersisted, value);
        storage.save(newPersisted);
        return repository.replacePersisted(newPersisted, descriptor);
    }

    private Object updateRuntime(ConfigFieldDescriptor descriptor, ConfigValueMutation mutation)
            throws ConfigValidationException
    {
        ConfigDocument newEffective = repository.effectiveDocument();
        Object value = validateValue(descriptor, mutation.apply(newEffective, descriptor));
        return repository.updateRuntime(descriptor, value);
    }

    private Object validateValue(ConfigFieldDescriptor descriptor, Object value) throws ConfigValidationException
    {
        Object normalized = descriptor.codec().normalizeValue(descriptor, value);
        semanticValidator.validate(descriptor, normalized);
        return normalized;
    }

    @FunctionalInterface
    private interface ConfigValueMutation
    {
        Object apply(ConfigDocument baseData, ConfigFieldDescriptor descriptor) throws ConfigValidationException;
    }

    private static Object externalEntry(ConfigFieldDescriptor descriptor, Object value) throws ConfigValidationException
    {
        if (value instanceof String s)
        {
            return descriptor.codec().parseEntry(descriptor, s);
        }
        if (value instanceof java.util.Map<?, ?> map && map.size() == 1)
        {
            java.util.Map.Entry<?, ?> entry = map.entrySet().iterator().next();
            return descriptor.codec().parseEntry(descriptor, entry.getKey() + "=" + entry.getValue());
        }
        throw new ConfigValidationException("Expected entry text or single-entry object");
    }

    private static Object externalRemovalEntry(ConfigFieldDescriptor descriptor, Object value) throws ConfigValidationException
    {
        if (value instanceof String s)
        {
            return s.contains("=") ? descriptor.codec().parseEntry(descriptor, s) : s;
        }
        return externalEntry(descriptor, value);
    }

    private static ConfigFieldDescriptor findDescriptor(ConfigEditRequest request)
    {
        ConfigPath path = request.path();
        return path == null ? null : ConfigSchema.find(path).orElse(null);
    }

    private static ConfigFieldDescriptor findListDescriptor(ConfigEditRequest request)
    {
        ConfigFieldDescriptor descriptor = findDescriptor(request);
        if (descriptor == null || !descriptor.codec().supportsCollectionEdits()) return null;
        return descriptor;
    }

    public record ConfigReloadResult(boolean success, boolean saved, ConfigRepairReport repairReport, String message) {}
}
