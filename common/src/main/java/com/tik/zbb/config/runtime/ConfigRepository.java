package com.tik.zbb.config.runtime;

import com.tik.zbb.config.ConfigDocument;
import com.tik.zbb.config.ConfigRuntime;
import com.tik.zbb.config.ConfigSnapshot;
import com.tik.zbb.config.edit.ConfigSemanticValidator;
import com.tik.zbb.config.io.ConfigStorage;
import com.tik.zbb.config.io.ConfigStorageException;
import com.tik.zbb.config.schema.ConfigFieldDescriptor;
import com.tik.zbb.config.schema.ConfigPath;
import com.tik.zbb.config.schema.ConfigSchema;
import com.tik.zbb.config.schema.ConfigValidationException;
import com.tik.zbb.utilities.ConfigUtilities;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public final class ConfigRepository
{
    private final ConfigPersistence persistence;
    private ConfigDocument persistedDocument;
    private ConfigDocument runtimeDocument;
    private ConfigResolver resolver = new ConfigResolver(ConfigSemanticValidator.NONE);
    private ConfigRuntime.BlockResolver blockResolver;
    private ConfigSnapshot snapshot;
    private long version;

    public ConfigRepository(ConfigStorage storage)
    {
        this(new ConfigDocument(), storage);
    }

    public ConfigRepository(ConfigDocument initialDocument, ConfigStorage storage)
    {
        persistence = new ConfigPersistence(storage);
        persistedDocument = ConfigUtilities.copyConfig(initialDocument);
        runtimeDocument = ConfigUtilities.copyConfig(initialDocument);
    }

    public synchronized ConfigLoadResult load()
    {
        if (blockResolver != null)
        {
            throw new IllegalStateException("Config runtime has already been activated");
        }

        ConfigPersistence.LoadResult loaded = persistence.load();
        if (loaded.success())
        {
            persistedDocument = ConfigUtilities.copyConfig(loaded.document());
            runtimeDocument = ConfigUtilities.copyConfig(loaded.document());
        }
        return result(loaded, new ConfigAvailabilityReport());
    }

    public synchronized ConfigLoadResult activate(ConfigRuntime.BlockResolver blockResolver, ConfigSemanticValidator semanticValidator)
    {
        this.blockResolver = Objects.requireNonNull(blockResolver, "blockResolver");
        resolver = new ConfigResolver(Objects.requireNonNull(semanticValidator, "semanticValidator"));

        ConfigPersistence.LoadResult loaded = persistence.load();
        if (loaded.success())
        {
            return resolveAndActivate(loaded);
        }

        ConfigResolver.Resolution resolved = resolver.resolve(persistedDocument);
        activate(persistedDocument, resolved.document());
        return result(loaded, resolved.report());
    }

    public synchronized ConfigLoadResult reload()
    {
        ConfigPersistence.LoadResult loaded = persistence.load();
        if (!loaded.success())
        {
            return result(loaded, new ConfigAvailabilityReport());
        }
        return resolveAndActivate(loaded);
    }

    public synchronized ConfigSnapshot snapshot()
    {
        if (snapshot == null)
        {
            throw new IllegalStateException("Config runtime has not been activated");
        }
        return snapshot;
    }

    public synchronized Object value(ConfigFieldDescriptor descriptor)
    {
        return descriptor.copyValue(descriptor.getValue(runtimeDocument));
    }

    public synchronized UpdateResult update(ConfigFieldDescriptor descriptor, ValueMutation mutation)
            throws ConfigValidationException, ConfigStorageException
    {
        Object currentValue = descriptor.getValue(runtimeDocument);
        Object candidate = mutation.apply(descriptor.copyValue(currentValue));
        Object updatedValue = descriptor.codec().normalizeValue(descriptor, candidate);
        resolver.validate(descriptor, updatedValue);

        Object originalPersistedValue = descriptor.getValue(persistedDocument);
        Object persistedValue = resolver.preserveUnavailable(originalPersistedValue, currentValue, updatedValue);

        if (Objects.equals(currentValue, updatedValue) && Objects.equals(originalPersistedValue, persistedValue))
        {
            return new UpdateResult(descriptor.copyValue(currentValue), false);
        }

        commitValue(descriptor, persistedValue, updatedValue);
        return new UpdateResult(descriptor.copyValue(updatedValue), true);
    }

    public synchronized UpdateResult replaceValue(ConfigFieldDescriptor descriptor, Object value)
            throws ConfigValidationException, ConfigStorageException
    {
        Object updatedValue = descriptor.codec().normalizeValue(descriptor, value);
        resolver.validate(descriptor, updatedValue);

        if (Objects.equals(descriptor.getValue(runtimeDocument), updatedValue)
                && Objects.equals(descriptor.getValue(persistedDocument), updatedValue))
        {
            return new UpdateResult(descriptor.copyValue(updatedValue), false);
        }

        commitValue(descriptor, updatedValue, updatedValue);
        return new UpdateResult(descriptor.copyValue(updatedValue), true);
    }

    public synchronized int resetToDefaults() throws ConfigStorageException
    {
        ConfigDocument defaults = new ConfigDocument();
        Set<ConfigPath> changedPaths = new LinkedHashSet<>();

        for (ConfigFieldDescriptor descriptor : ConfigSchema.descriptors())
        {
            Object defaultValue = descriptor.defaultValue();
            if (!Objects.equals(descriptor.getValue(runtimeDocument), defaultValue)
                    || !Objects.equals(descriptor.getValue(persistedDocument), defaultValue))
            {
                changedPaths.add(descriptor.path());
            }
        }

        if (changedPaths.isEmpty()) return 0;

        persistence.save(defaults);
        activate(defaults, defaults);
        return changedPaths.size();
    }

    private ConfigLoadResult resolveAndActivate(ConfigPersistence.LoadResult loaded)
    {
        ConfigResolver.Resolution resolved = resolver.resolve(loaded.document());
        activate(loaded.document(), resolved.document());
        return result(loaded, resolved.report());
    }

    private static ConfigLoadResult result(ConfigPersistence.LoadResult loaded, ConfigAvailabilityReport availabilityReport)
    {
        return new ConfigLoadResult(
                loaded.success(),
                loaded.saved(),
                loaded.fileReport(),
                availabilityReport,
                loaded.message()
        );
    }

    private void commitValue(ConfigFieldDescriptor descriptor, Object fileValue, Object activeValue)
            throws ConfigStorageException
    {
        ConfigDocument updatedFileDocument = ConfigUtilities.copyConfig(persistedDocument);
        descriptor.setValue(updatedFileDocument, fileValue);
        persistence.save(updatedFileDocument);

        ConfigDocument updatedActiveDocument = ConfigUtilities.copyConfig(runtimeDocument);
        descriptor.setValue(updatedActiveDocument, activeValue);
        activate(updatedFileDocument, updatedActiveDocument);
    }

    private void activate(ConfigDocument newFileDocument, ConfigDocument newRuntimeDocument)
    {
        persistedDocument = ConfigUtilities.copyConfig(newFileDocument);
        runtimeDocument = ConfigUtilities.copyConfig(newRuntimeDocument);
        if (blockResolver == null) return;

        version++;
        snapshot = ConfigSnapshot.create(runtimeDocument, version, blockResolver);
    }

    @FunctionalInterface
    public interface ValueMutation
    {
        Object apply(Object currentValue) throws ConfigValidationException;
    }

    public record UpdateResult(Object value, boolean changed) {}
}
