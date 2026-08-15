package com.tik.zbb.config;

import com.electronwill.nightconfig.core.serde.ObjectDeserializer;
import com.tik.zbb.Constants;
import com.tik.zbb.config.edit.ConfigEditRequest;
import com.tik.zbb.config.edit.ConfigEditResult;
import com.tik.zbb.config.edit.ConfigEditService;
import com.tik.zbb.config.edit.ConfigWriteMode;
import com.tik.zbb.config.edit.MinecraftConfigSemanticValidator;
import com.tik.zbb.config.io.ConfigDocumentNormalizer;
import com.tik.zbb.config.io.ConfigFileStore;
import com.tik.zbb.config.runtime.ConfigRepository;
import com.tik.zbb.config.schema.ConfigFieldDescriptor;
import com.tik.zbb.config.schema.ConfigPath;
import com.tik.zbb.platform.Services;
import net.minecraft.server.MinecraftServer;

import java.nio.file.Path;
import java.util.Map;

public final class ConfigManager
{
    private static final ObjectDeserializer DESERIALIZER = ObjectDeserializer.standard();

    private static volatile ConfigEditService EDIT_SERVICE;

    public static synchronized void init()
    {
        Path configPath = Services.PLATFORM.getConfigDir().resolve("zombies-break-build.toml");

        ConfigFileStore fileStore = new ConfigFileStore(
                configPath,
                new ConfigDocumentNormalizer(DESERIALIZER)
        );
        ConfigRepository repository = new ConfigRepository(new ConfigDocument());

        EDIT_SERVICE = new ConfigEditService(repository, fileStore);
        logReloadResult(service().bootstrapFromFile());
    }

    public static synchronized void startRuntime(MinecraftServer server)
    {
        logReloadResult(service().startRuntime(
                ConfigGame.BlockResolver.MINECRAFT,
                new MinecraftConfigSemanticValidator(server.registryAccess())
        ));
    }

    public static ConfigSnapshot getConfigSnapshot()
    {
        return service().snapshot();
    }

    public static Object getEffectiveValue(ConfigFieldDescriptor descriptor)
    {
        return service().effectiveValue(descriptor);
    }

    public static Object getValueForMode(ConfigFieldDescriptor descriptor, ConfigWriteMode writeMode)
    {
        return service().valueForMode(descriptor, writeMode);
    }

    public static Map<ConfigPath, Object> getRuntimeOverrides()
    {
        return service().runtimeOverrides();
    }

    public static ConfigEditResult edit(ConfigEditRequest request)
    {
        return service().edit(request);
    }

    public static ConfigEditResult editRaw(ConfigEditRequest request)
    {
        return service().editRaw(request);
    }

    public static synchronized ConfigEditService.ConfigReloadResult reload()
    {
        ConfigEditService.ConfigReloadResult result = service().reloadFromFile();
        logReloadResult(result);
        return result;
    }

    private static void logReloadResult(ConfigEditService.ConfigReloadResult result)
    {
        if (result.repairReport() != null && result.repairReport().hasEntries())
        {
            for (String entry : result.repairReport().entries())
            {
                Constants.LOG.warn("Repaired config: {}", entry);
            }
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
}
