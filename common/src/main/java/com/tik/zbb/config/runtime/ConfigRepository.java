package com.tik.zbb.config.runtime;

import com.tik.zbb.config.ConfigDocument;
import com.tik.zbb.config.ConfigGame;
import com.tik.zbb.config.ConfigSnapshot;
import com.tik.zbb.config.document.ConfigDocumentCopier;
import com.tik.zbb.config.schema.ConfigFieldDescriptor;

import java.util.Objects;

public final class ConfigRepository
{
    private ConfigDocument persistedData;
    private ConfigGame.BlockResolver blockResolver;
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
        return descriptor.copyValue(descriptor.getValue(persistedData));
    }

    public synchronized ConfigDocument persistedDocument()
    {
        return ConfigDocumentCopier.copy(persistedData);
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
        persistedData = ConfigDocumentCopier.copy(data);
        rebuildEffectiveSnapshot();
        return changedDescriptor == null ? null : effectiveValue(changedDescriptor);
    }

    public synchronized void replacePersisted(ConfigDocument data)
    {
        replacePersisted(data, null);
    }

    private void rebuildEffectiveSnapshot()
    {
        version++;
        snapshot = ConfigSnapshot.create(persistedData, version, blockResolver);
    }
}
