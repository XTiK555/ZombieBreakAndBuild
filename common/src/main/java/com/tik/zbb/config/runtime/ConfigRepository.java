package com.tik.zbb.config.runtime;

import com.tik.zbb.config.ConfigDocument;
import com.tik.zbb.config.ConfigGame;
import com.tik.zbb.config.ConfigSnapshot;
import com.tik.zbb.config.document.ConfigDocumentCopier;
import com.tik.zbb.config.schema.ConfigFieldDescriptor;
import com.tik.zbb.config.schema.ConfigPath;
import com.tik.zbb.config.schema.ConfigSchema;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ConfigRepository
{
    private ConfigDocument persistedData;
    private final Map<ConfigPath, Object> runtimeOverrides = new LinkedHashMap<>();
    private final ConfigGame.BlockResolver blockResolver;
    private ConfigDocument effectiveData;
    private ConfigSnapshot snapshot;
    private long version;

    public ConfigRepository(ConfigDocument initialData)
    {
        this(initialData, ConfigGame.BlockResolver.NONE);
    }

    public ConfigRepository(ConfigDocument initialData, ConfigGame.BlockResolver blockResolver)
    {
        this.blockResolver = blockResolver;
        this.persistedData = ConfigDocumentCopier.copy(initialData);
        rebuildEffectiveSnapshot();
    }

    public synchronized ConfigSnapshot snapshot()
    {
        return snapshot;
    }

    public synchronized Object effectiveValue(ConfigFieldDescriptor descriptor)
    {
        return descriptor.copyValue(descriptor.getValue(effectiveData));
    }

    public synchronized ConfigDocument persistedDocument()
    {
        return ConfigDocumentCopier.copy(persistedData);
    }

    public synchronized ConfigDocument effectiveDocument()
    {
        return ConfigDocumentCopier.copy(effectiveData);
    }

    public synchronized Object replacePersisted(ConfigDocument data, ConfigFieldDescriptor changedDescriptor)
    {
        mutateAndRebuild(() ->
        {
            persistedData = ConfigDocumentCopier.copy(data);
            if (changedDescriptor == null)
            {
                runtimeOverrides.clear();
            }
            else
            {
                runtimeOverrides.keySet().removeIf(runtimePath -> runtimePath.isDescendantOf(changedDescriptor.path()));
            }
        });
        return changedDescriptor == null ? null : effectiveValue(changedDescriptor);
    }

    public synchronized void replacePersisted(ConfigDocument data)
    {
        replacePersisted(data, null);
    }

    public synchronized Object updateRuntime(ConfigFieldDescriptor descriptor, Object value)
    {
        runtimeOverrides.put(descriptor.path(), descriptor.copyValue(value));
        rebuildEffectiveSnapshot();
        return descriptor.copyValue(descriptor.getValue(effectiveData));
    }

    public synchronized int resetRuntimeOverrides()
    {
        int count = runtimeOverrides.size();
        mutateAndRebuild(() ->
        {
            Map<ConfigPath, Object> defaults = new LinkedHashMap<>();
            for (ConfigPath path : runtimeOverrides.keySet())
            {
                ConfigSchema.find(path).ifPresent(descriptor ->
                        defaults.put(path, descriptor.defaultValue()));
            }

            runtimeOverrides.clear();
            for (Map.Entry<ConfigPath, Object> entry : defaults.entrySet())
            {
                ConfigSchema.find(entry.getKey()).ifPresent(descriptor ->
                        runtimeOverrides.put(entry.getKey(), descriptor.copyValue(entry.getValue())));
            }
        });
        return count;
    }

    public synchronized int discard(ConfigPath path)
    {
        int before = runtimeOverrides.size();
        mutateAndRebuild(() ->
                runtimeOverrides.keySet().removeIf(runtimePath -> runtimePath.isDescendantOf(path)));
        return before - runtimeOverrides.size();
    }

    public synchronized int discardAll()
    {
        int count = runtimeOverrides.size();
        mutateAndRebuild(runtimeOverrides::clear);
        return count;
    }

    private void mutateAndRebuild(Runnable mutation)
    {
        mutation.run();
        rebuildEffectiveSnapshot();
    }

    private void rebuildEffectiveSnapshot()
    {
        effectiveData = ConfigDocumentCopier.copy(persistedData);
        for (Map.Entry<ConfigPath, Object> entry : runtimeOverrides.entrySet())
        {
            ConfigSchema.find(entry.getKey()).ifPresent(descriptor ->
                    descriptor.setValue(effectiveData, entry.getValue()));
        }
        version++;
        snapshot = ConfigSnapshot.create(effectiveData, version, blockResolver);
    }
}
