package com.tik.zbb.config.edit.handler;

import com.tik.zbb.Constants;
import com.tik.zbb.config.ConfigData;
import com.tik.zbb.config.edit.*;
import com.tik.zbb.config.io.ConfigPersistenceException;
import com.tik.zbb.config.schema.ConfigPath;
import com.tik.zbb.config.schema.ConfigSchema;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ResetAllConfigEditHandler implements ConfigEditHandler
{
    @Override
    public ConfigEditOperation operation()
    {
        return ConfigEditOperation.RESET_ALL;
    }

    @Override
    public ConfigEditResult handle(ConfigEditRequest request, ConfigEditContext context)
    {
        if (request.writeMode() == ConfigWriteMode.PERSISTENT)
        {
            ConfigData defaults = new ConfigData();
            try
            {
                context.fileStore().save(defaults);
            }
            catch (ConfigPersistenceException e)
            {
                Constants.LOG.error("Failed to save config", e);
                return ConfigEditResult.failure(request, "Failed to save config: " + e.getMessage());
            }

            context.repository().replacePersisted(defaults);
            return ConfigEditResult.success(request, null, true, ConfigSchema.descriptors().size(), "reset all");
        }

        Map<ConfigPath, Object> overrides = new LinkedHashMap<>();
        for (ConfigPath path : context.repository().runtimeOverridePaths())
        {
            ConfigSchema.find(path).ifPresent(descriptor ->
                    overrides.put(path, descriptor.defaultValue()));
        }

        context.repository().replaceRuntimeOverrides(overrides);
        return ConfigEditResult.success(request, null, false, overrides.size(), "reset all temporary value(s)");
    }
}
