package com.tik.zbb.config.edit;

import com.tik.zbb.Constants;
import com.tik.zbb.config.ConfigDocument;
import com.tik.zbb.config.ConfigGame;
import com.tik.zbb.config.ConfigSnapshot;
import com.tik.zbb.config.io.ConfigStorage;
import com.tik.zbb.config.io.ConfigStorageException;
import com.tik.zbb.config.runtime.ConfigRepository;
import com.tik.zbb.config.schema.*;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class ConfigEditService
{
    private final ConfigRepository repository;
    private final ConfigStorage storage;
    private ConfigSemanticValidator semanticValidator;

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

    public Map<ConfigPath, Object> runtimeOverrides()
    {
        return repository.runtimeOverrides();
    }

    public synchronized ConfigReloadResult bootstrapFromFile()
    {
        return loadFromFile(ConfigSemanticValidator.NONE);
    }

    public synchronized ConfigReloadResult reloadFromFile()
    {
        return loadFromFile(semanticValidator);
    }

    public synchronized ConfigReloadResult startRuntime(ConfigGame.BlockResolver blockResolver)
    {
        return startRuntime(blockResolver, semanticValidator);
    }

    public synchronized ConfigReloadResult startRuntime(ConfigGame.BlockResolver blockResolver, ConfigSemanticValidator runtimeValidator)
    {
        semanticValidator = Objects.requireNonNull(runtimeValidator, "runtimeValidator");
        ConfigReloadResult result = loadFromFile(semanticValidator);
        repository.activateBlockResolution(blockResolver);
        return result;
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
            ConfigDocument persisted = repository.persistedDocument();
            Map<ConfigPath, Object> runtimeOverrides = repository.runtimeOverrides();
            Set<ConfigPath> changedPaths = new LinkedHashSet<>();
            boolean persistedChanged = false;

            for (ConfigFieldDescriptor descriptor : ConfigSchema.descriptors())
            {
                if (!Objects.equals(descriptor.getValue(persisted), descriptor.defaultValue()))
                {
                    persistedChanged = true;
                    changedPaths.add(descriptor.path());
                }
                if (runtimeOverrides.containsKey(descriptor.path()))
                {
                    changedPaths.add(descriptor.path());
                }
            }

            if (changedPaths.isEmpty())
            {
                return ConfigEditResult.unchanged(request, null, true);
            }

            try
            {
                if (persistedChanged)
                {
                    storage.save(defaults);
                    repository.replacePersisted(defaults);
                }
                else
                {
                    repository.discardAll();
                }
            }
            catch (ConfigStorageException e)
            {
                Constants.LOG.error("Failed to save config", e);
                return ConfigEditResult.failure(request, "Failed to save config: " + e.getMessage());
            }

            return ConfigEditResult.success(request, null, true, changedPaths.size(), "reset all");
        }

        int count = repository.resetRuntimeOverrides();
        return countResult(request, count, "reset all temporary value(s)");
    }

    private ConfigEditResult discard(ConfigEditRequest request)
    {
        if (!ConfigSchema.hasPathOrSection(request.path()))
        {
            return ConfigEditResult.failure(request, "Unknown config path or section: " + request.path());
        }

        int count = repository.discard(request.path());
        return countResult(request, count, "discarded " + count + " temporary value(s)");
    }

    private ConfigEditResult discardAll(ConfigEditRequest request)
    {
        int count = repository.discardAll();
        return countResult(request, count, "discarded " + count + " temporary value(s)");
    }

    private ConfigEditResult countResult(ConfigEditRequest request, int count, String message)
    {
        return count == 0
                ? ConfigEditResult.unchanged(request, null, false)
                : ConfigEditResult.success(request, null, false, count, message);
    }

    private ConfigEditResult applyValue(
            ConfigEditRequest request,
            ConfigFieldDescriptor descriptor,
            ConfigValueMutation mutation
    )
    {
        try
        {
            ConfigMutationResult mutationResult = request.writeMode() == ConfigWriteMode.PERSISTENT
                    ? updatePersistent(descriptor, mutation)
                    : updateRuntime(descriptor, mutation);
            if (!mutationResult.changed())
            {
                return ConfigEditResult.unchanged(
                        request,
                        mutationResult.effectiveValue(),
                        request.writeMode() == ConfigWriteMode.PERSISTENT
                );
            }
            return ConfigEditResult.success(request, mutationResult.effectiveValue(),
                    request.writeMode() == ConfigWriteMode.PERSISTENT, 1, "updated");
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

    private ConfigMutationResult updatePersistent(ConfigFieldDescriptor descriptor, ConfigValueMutation mutation)
            throws ConfigValidationException, ConfigStorageException
    {
        ConfigDocument newPersisted = repository.persistedDocument();
        Object value = validateValue(descriptor, mutation.apply(newPersisted, descriptor));
        if (Objects.equals(descriptor.getValue(newPersisted), value))
        {
            boolean discardedRuntimeOverride = repository.discard(descriptor.path()) > 0;
            return new ConfigMutationResult(repository.effectiveValue(descriptor), discardedRuntimeOverride);
        }

        descriptor.setValue(newPersisted, value);
        storage.save(newPersisted);
        return new ConfigMutationResult(repository.replacePersisted(newPersisted, descriptor), true);
    }

    private ConfigMutationResult updateRuntime(ConfigFieldDescriptor descriptor, ConfigValueMutation mutation)
            throws ConfigValidationException
    {
        ConfigDocument newEffective = repository.effectiveDocument();
        Object value = validateValue(descriptor, mutation.apply(newEffective, descriptor));
        if (Objects.equals(descriptor.getValue(newEffective), value))
        {
            return new ConfigMutationResult(repository.effectiveValue(descriptor), false);
        }
        return new ConfigMutationResult(repository.updateRuntime(descriptor, value), true);
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

    private record ConfigMutationResult(Object effectiveValue, boolean changed) {}

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
