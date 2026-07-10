package com.tik.zbb.config.edit;

import com.tik.zbb.Constants;
import com.tik.zbb.config.ConfigData;
import com.tik.zbb.config.io.ConfigFileStore;
import com.tik.zbb.config.io.ConfigPersistenceException;
import com.tik.zbb.config.runtime.ConfigRepository;
import com.tik.zbb.config.schema.ConfigFieldDescriptor;
import com.tik.zbb.config.schema.ConfigSchema;
import com.tik.zbb.config.schema.ConfigValidationException;
import com.tik.zbb.config.schema.ConfigValueKind;

import java.util.Set;

public record ConfigEditContext(ConfigRepository repository, ConfigFileStore fileStore)
{
    public ConfigData baseData(ConfigWriteMode writeMode)
    {
        return writeMode == ConfigWriteMode.PERSISTENT ? repository.persistedCopy() : repository.effectiveCopy();
    }

    public ConfigFieldDescriptor findDescriptor(ConfigEditRequest request)
    {
        return request.path() == null ? null : ConfigSchema.find(request.path()).orElse(null);
    }

    public ConfigFieldDescriptor findListDescriptor(ConfigEditRequest request)
    {
        ConfigFieldDescriptor descriptor = findDescriptor(request);
        if (descriptor == null || descriptor.kind() != ConfigValueKind.STRING_LIST) return null;
        return descriptor;
    }

    public ConfigEditResult applyValue(ConfigEditRequest request, ConfigFieldDescriptor descriptor, Object value)
    {
        try
        {
            descriptor.codec().validateValue(descriptor, value);
        }
        catch (ConfigValidationException e)
        {
            return ConfigEditResult.failure(request, descriptor.path() + ": " + e.getMessage());
        }

        if (request.writeMode() == ConfigWriteMode.PERSISTENT)
        {
            ConfigData newPersisted = repository.persistedCopy();
            descriptor.setValue(newPersisted, value);

            try
            {
                fileStore.save(newPersisted);
            }
            catch (ConfigPersistenceException e)
            {
                Constants.LOG.error("Failed to save config", e);
                return ConfigEditResult.failure(request, "Failed to save config: " + e.getMessage());
            }

            repository.commitPersistent(newPersisted, Set.of(descriptor.path()));
            return ConfigEditResult.success(request, repository.effectiveValue(descriptor), true, 1, "updated");
        }

        repository.putRuntimeOverride(descriptor, value);
        return ConfigEditResult.success(request, repository.effectiveValue(descriptor), false, 1, "updated");
    }
}
