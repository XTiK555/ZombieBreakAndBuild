package com.tik.zbb.config.edit;

import com.tik.zbb.Constants;
import com.tik.zbb.config.io.ConfigStorageException;
import com.tik.zbb.config.runtime.ConfigRepository;
import com.tik.zbb.config.schema.ConfigFieldDescriptor;
import com.tik.zbb.config.schema.ConfigPath;
import com.tik.zbb.config.schema.ConfigSchema;
import com.tik.zbb.config.schema.ConfigValidationException;

import java.util.Map;
import java.util.Objects;


public final class ConfigEditService
{
    private final ConfigRepository repository;

    public ConfigEditService(ConfigRepository repository)
    {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public ConfigEditResult edit(ConfigEditRequest request)
    {
        return switch (request.operation())
        {
            case SET -> set(request);
            case ADD -> add(request);
            case REMOVE -> remove(request);
            case CLEAR -> clear(request);
            case RESET_TO_DEFAULT -> reset(request);
            case RESET_ALL_TO_DEFAULTS -> resetAll(request);
        };
    }

    public ConfigEditResult editRaw(ConfigEditRequest request)
    {
        if (request.operation() == ConfigEditOperation.RESET_ALL_TO_DEFAULTS)
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
                case SET -> request.withValue(descriptor.codec().parseText(descriptor, String.valueOf(request.value())));
                case ADD -> request.withValue(externalEntry(descriptor, request.value()));
                case REMOVE -> request.withValue(externalRemovalEntry(descriptor, request.value()));
                case CLEAR, RESET_TO_DEFAULT, RESET_ALL_TO_DEFAULTS -> request;
            };
            return edit(typedRequest);
        }
        catch (ConfigValidationException e)
        {
            return ConfigEditResult.failure(request, descriptor.path() + ": " + e.getMessage());
        }
    }

    private ConfigEditResult set(ConfigEditRequest request)
    {
        ConfigFieldDescriptor descriptor = findDescriptor(request);
        if (descriptor == null) return ConfigEditResult.failure(request, "Unknown config path: " + request.path());

        return applyValue(request, descriptor, currentValue -> request.value());
    }

    private ConfigEditResult add(ConfigEditRequest request)
    {
        ConfigFieldDescriptor descriptor = findCollectionDescriptor(request);
        if (descriptor == null) return ConfigEditResult.failure(request, request.path() + " is not a collection");

        return applyValue(request, descriptor, currentValue ->
                descriptor.codec().addEntry(descriptor, currentValue, request.value()));
    }

    private ConfigEditResult remove(ConfigEditRequest request)
    {
        ConfigFieldDescriptor descriptor = findCollectionDescriptor(request);
        if (descriptor == null) return ConfigEditResult.failure(request, request.path() + " is not a collection");

        return applyValue(request, descriptor, currentValue ->
                descriptor.codec().removeEntry(descriptor, currentValue, request.value()));
    }

    private ConfigEditResult clear(ConfigEditRequest request)
    {
        ConfigFieldDescriptor descriptor = findCollectionDescriptor(request);
        if (descriptor == null) return ConfigEditResult.failure(request, request.path() + " is not a collection");

        return replaceValue(request, descriptor, descriptor.codec().emptyValue(descriptor));
    }

    private ConfigEditResult reset(ConfigEditRequest request)
    {
        ConfigFieldDescriptor descriptor = findDescriptor(request);
        if (descriptor == null) return ConfigEditResult.failure(request, "Unknown config path: " + request.path());

        return replaceValue(request, descriptor, descriptor.defaultValue());
    }

    private ConfigEditResult resetAll(ConfigEditRequest request)
    {
        try
        {
            int changedCount = repository.resetToDefaults();
            if (changedCount == 0) return ConfigEditResult.unchanged(request, null);
            return ConfigEditResult.success(request, null, changedCount, "reset all");
        }
        catch (ConfigStorageException e)
        {
            Constants.LOG.error("Failed to save config", e);
            return ConfigEditResult.failure(request, "Failed to save config: " + e.getMessage());
        }
    }

    private ConfigEditResult applyValue(ConfigEditRequest request, ConfigFieldDescriptor descriptor, ConfigRepository.ValueMutation mutation)
    {
        try
        {
            ConfigRepository.UpdateResult result = repository.update(descriptor, mutation);
            if (!result.changed()) return ConfigEditResult.unchanged(request, result.value());
            return ConfigEditResult.success(request, result.value(), 1, "updated");
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

    private ConfigEditResult replaceValue(ConfigEditRequest request, ConfigFieldDescriptor descriptor, Object value)
    {
        try
        {
            ConfigRepository.UpdateResult result = repository.replaceValue(descriptor, value);
            if (!result.changed()) return ConfigEditResult.unchanged(request, result.value());
            return ConfigEditResult.success(request, result.value(), 1, "updated");
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

    private static Object externalEntry(ConfigFieldDescriptor descriptor, Object value) throws ConfigValidationException
    {
        if (value instanceof String s)
        {
            return descriptor.codec().parseEntry(descriptor, s);
        }
        if (value instanceof Map<?, ?> map && map.size() == 1)
        {
            Map.Entry<?, ?> entry = map.entrySet().iterator().next();
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

    private static ConfigFieldDescriptor findCollectionDescriptor(ConfigEditRequest request)
    {
        ConfigFieldDescriptor descriptor = findDescriptor(request);
        if (descriptor == null || !descriptor.codec().supportsCollectionEdits()) return null;
        return descriptor;
    }
}
