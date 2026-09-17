package com.tik.zbb.config;

import com.electronwill.nightconfig.core.serde.ObjectDeserializer;
import com.tik.zbb.Constants;
import com.tik.zbb.config.edit.ConfigEditRequest;
import com.tik.zbb.config.edit.ConfigEditResult;
import com.tik.zbb.config.edit.ConfigEditService;
import com.tik.zbb.config.edit.MinecraftConfigSemanticValidator;
import com.tik.zbb.config.io.ConfigDocumentNormalizer;
import com.tik.zbb.config.io.ConfigFileStore;
import com.tik.zbb.config.runtime.ConfigLoadResult;
import com.tik.zbb.config.runtime.ConfigRepository;
import com.tik.zbb.config.schema.ConfigFieldDescriptor;
import com.tik.zbb.platform.Services;
import net.minecraft.server.MinecraftServer;

import java.nio.file.Path;

public final class ConfigManager
{
    private static final ObjectDeserializer DESERIALIZER = ObjectDeserializer.standard();

    private static volatile ConfigRepository REPOSITORY;
    private static volatile ConfigEditService EDIT_SERVICE;

    public static synchronized void init()
    {
        Path configPath = Services.PLATFORM.getConfigDir().resolve("zombies-break-build.toml");

        ConfigFileStore fileStore = new ConfigFileStore(
                configPath,
                new ConfigDocumentNormalizer(DESERIALIZER)
        );
        ConfigRepository repository = new ConfigRepository(fileStore);

        REPOSITORY = repository;
        EDIT_SERVICE = new ConfigEditService(repository);
        logReloadResult(repository().load());
    }

    public static synchronized void activate(MinecraftServer server)
    {
        logReloadResult(repository().activate(
                ConfigRuntime.BlockResolver.MINECRAFT,
                new MinecraftConfigSemanticValidator(server.registryAccess())
        ));
    }

    public static ConfigSnapshot getConfigSnapshot()
    {
        return repository().snapshot();
    }

    public static Object getValue(ConfigFieldDescriptor descriptor)
    {
        return repository().value(descriptor);
    }

    public static ConfigEditResult edit(ConfigEditRequest request)
    {
        return service().edit(request);
    }

    public static ConfigEditResult editRaw(ConfigEditRequest request)
    {
        return service().editRaw(request);
    }

    public static synchronized ConfigLoadResult reload()
    {
        ConfigLoadResult result = repository().reload();
        logReloadResult(result);
        return result;
    }

    private static void logReloadResult(ConfigLoadResult result)
    {
        for (String entry : result.fileReport().missingValues())
        {
            Constants.LOG.warn("Added missing config value: {}", entry);
        }
        for (String entry : result.fileReport().invalidValues())
        {
            Constants.LOG.warn("Replaced invalid config value: {}", entry);
        }
        for (String entry : result.availabilityReport().entries())
        {
            Constants.LOG.warn("Unavailable config value: {}", entry);
        }

        if (result.success())
        {
            Constants.LOG.info(result.message());
        }
        else
        {
            Constants.LOG.warn(result.message());
        }
    }

    private static ConfigEditService service()
    {
        ConfigEditService service = EDIT_SERVICE;
        if (service == null)
        {
            throw new IllegalStateException("ConfigManager used before init");
        }
        return service;
    }

    private static ConfigRepository repository()
    {
        ConfigRepository repository = REPOSITORY;
        if (repository == null)
        {
            throw new IllegalStateException("ConfigManager used before init");
        }
        return repository;
    }
}
