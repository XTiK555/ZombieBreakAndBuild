package com.tik.zbb.config.runtime;

import com.tik.zbb.config.ConfigDocument;
import com.tik.zbb.config.ConfigGame;
import com.tik.zbb.config.ConfigSnapshot;
import com.tik.zbb.config.document.ConfigDocumentCopier;
import com.tik.zbb.config.schema.ConfigFieldDescriptor;
import com.tik.zbb.config.schema.ConfigPath;
import com.tik.zbb.config.schema.ConfigSchema;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class ConfigRepository
{
    private ConfigDocument persistedData;
    private final Map<ConfigPath, Object> runtimeOverrides = new LinkedHashMap<>();
    private ConfigGame.BlockResolver blockResolver;
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

    public synchronized Object persistedValue(ConfigFieldDescriptor descriptor)
    {
        return descriptor.copyValue(descriptor.getValue(persistedData));
    }

    public synchronized ConfigDocument persistedDocument()
    {
        return ConfigDocumentCopier.copy(persistedData);
    }

    public synchronized ConfigDocument effectiveDocument()
    {
        return ConfigDocumentCopier.copy(effectiveData);
    }

    public synchronized Map<ConfigPath, Object> runtimeOverrides()
    {
        Map<ConfigPath, Object> copy = new LinkedHashMap<>();
        for (Map.Entry<ConfigPath, Object> entry : runtimeOverrides.entrySet())
        {
            ConfigSchema.find(entry.getKey()).ifPresent(descriptor ->
                    copy.put(entry.getKey(), descriptor.copyValue(entry.getValue())));
        }
        return Collections.unmodifiableMap(copy);
    }

    public synchronized void activateBlockResolution(ConfigGame.BlockResolver resolver)
    {
        Objects.requireNonNull(resolver, "resolver");
        if (blockResolver == resolver) return;

        blockResolver = resolver;
        rebuildEffectiveSnapshot();
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
        Object copiedValue = descriptor.copyValue(value);
        if (!runtimeOverrides.containsKey(descriptor.path())
                || !Objects.equals(runtimeOverrides.get(descriptor.path()), copiedValue))
        {
            runtimeOverrides.put(descriptor.path(), copiedValue);
            rebuildEffectiveSnapshot();
        }
        return descriptor.copyValue(descriptor.getValue(effectiveData));
    }

    public synchronized int resetRuntimeOverrides()
    {
        Map<ConfigPath, Object> defaults = new LinkedHashMap<>();
        for (ConfigFieldDescriptor descriptor : ConfigSchema.descriptors())
        {
            defaults.put(descriptor.path(), descriptor.defaultValue());
        }

        int count = changedEntryCount(runtimeOverrides, defaults);
        if (count == 0) return 0;

        mutateAndRebuild(() ->
        {
            runtimeOverrides.clear();
            runtimeOverrides.putAll(defaults);
        });
        return count;
    }

    public synchronized int discard(ConfigPath path)
    {
        int before = runtimeOverrides.size();
        runtimeOverrides.keySet().removeIf(runtimePath -> runtimePath.isDescendantOf(path));
        int count = before - runtimeOverrides.size();
        if (count > 0) rebuildEffectiveSnapshot();
        return count;
    }

    public synchronized int discardAll()
    {
        int count = runtimeOverrides.size();
        if (count > 0) mutateAndRebuild(runtimeOverrides::clear);
        return count;
    }

    private static int changedEntryCount(Map<ConfigPath, Object> current, Map<ConfigPath, Object> replacement)
    {
        int count = 0;
        for (Map.Entry<ConfigPath, Object> entry : replacement.entrySet())
        {
            if (!current.containsKey(entry.getKey()) || !Objects.equals(current.get(entry.getKey()), entry.getValue()))
            {
                count++;
            }
        }
        for (ConfigPath path : current.keySet())
        {
            if (!replacement.containsKey(path)) count++;
        }
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
