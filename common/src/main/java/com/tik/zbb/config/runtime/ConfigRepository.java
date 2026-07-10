package com.tik.zbb.config.runtime;

import com.tik.zbb.config.ConfigData;
import com.tik.zbb.config.ConfigDataCopier;
import com.tik.zbb.config.ConfigSnapshot;
import com.tik.zbb.config.schema.ConfigFieldDescriptor;
import com.tik.zbb.config.schema.ConfigPath;
import com.tik.zbb.config.schema.ConfigSchema;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ConfigRepository
{
    private ConfigData persistedData;
    private final Map<ConfigPath, Object> runtimeOverrides = new LinkedHashMap<>();
    private ConfigData effectiveData;
    private ConfigSnapshot snapshot;
    private long version;

    public ConfigRepository(ConfigData initialData)
    {
        this.persistedData = ConfigDataCopier.copy(initialData);
        rebuildEffectiveSnapshot();
    }

    public synchronized ConfigSnapshot snapshot()
    {
        return snapshot;
    }

    public synchronized ConfigData persistedCopy()
    {
        return ConfigDataCopier.copy(persistedData);
    }

    public synchronized ConfigData effectiveCopy()
    {
        return ConfigDataCopier.copy(effectiveData);
    }

    public synchronized Object effectiveValue(ConfigFieldDescriptor descriptor)
    {
        return descriptor.copyValue(descriptor.getValue(effectiveData));
    }

    public synchronized void replacePersisted(ConfigData data)
    {
        mutateAndRebuild(() ->
        {
            persistedData = ConfigDataCopier.copy(data);
            runtimeOverrides.clear();
        });
    }

    public synchronized void commitPersistent(ConfigData data, Set<ConfigPath> pathsToClear)
    {
        mutateAndRebuild(() ->
        {
            persistedData = ConfigDataCopier.copy(data);
            for (ConfigPath path : pathsToClear)
            {
                runtimeOverrides.keySet().removeIf(runtimePath -> runtimePath.isDescendantOf(path));
            }
        });
    }

    public synchronized void replaceRuntimeOverrides(Map<ConfigPath, Object> overrides)
    {
        mutateAndRebuild(() ->
        {
            runtimeOverrides.clear();
            for (Map.Entry<ConfigPath, Object> entry : overrides.entrySet())
            {
                ConfigSchema.find(entry.getKey()).ifPresent(descriptor ->
                        runtimeOverrides.put(entry.getKey(), descriptor.copyValue(entry.getValue())));
            }
        });
    }

    public synchronized List<ConfigPath> runtimeOverridePaths()
    {
        return new ArrayList<>(runtimeOverrides.keySet());
    }

    public synchronized void putRuntimeOverride(ConfigFieldDescriptor descriptor, Object value)
    {
        mutateAndRebuild(() ->
                runtimeOverrides.put(descriptor.path(), descriptor.copyValue(value)));
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
        effectiveData = ConfigDataCopier.copy(persistedData);
        for (Map.Entry<ConfigPath, Object> entry : runtimeOverrides.entrySet())
        {
            ConfigSchema.find(entry.getKey()).ifPresent(descriptor ->
                    descriptor.setValue(effectiveData, entry.getValue()));
        }
        version++;
        snapshot = ConfigSnapshot.create(effectiveData, version);
    }
}
