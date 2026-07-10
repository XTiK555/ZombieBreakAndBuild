package com.tik.zbb.config;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.serde.ObjectDeserializer;
import com.electronwill.nightconfig.core.serde.ObjectSerializer;
import com.tik.zbb.Constants;
import com.tik.zbb.config.edit.ConfigEditRequest;
import com.tik.zbb.config.edit.ConfigEditResult;
import com.tik.zbb.config.edit.ConfigEditService;
import com.tik.zbb.config.io.ConfigDocumentNormalizer;
import com.tik.zbb.config.io.ConfigFileStore;
import com.tik.zbb.config.runtime.ConfigRepository;
import com.tik.zbb.config.schema.ConfigFieldDescriptor;
import com.tik.zbb.platform.Services;

import java.nio.file.Path;

public final class ConfigManager
{
    private static final ObjectSerializer SERIALIZER = ObjectSerializer.standard();
    private static final ObjectDeserializer DESERIALIZER = ObjectDeserializer.standard();

    private static volatile ConfigEditService EDIT_SERVICE;

    public static synchronized void init()
    {
        Config.setInsertionOrderPreserved(true);

        String modName = Constants.MOD_NAME.replaceAll("\\s", "-").toLowerCase();
        Path configPath = Services.PLATFORM.getConfigDir().resolve(modName + ".toml");

        ConfigFileStore fileStore = new ConfigFileStore(configPath, SERIALIZER);
        ConfigDocumentNormalizer normalizer = new ConfigDocumentNormalizer(DESERIALIZER);
        ConfigRepository repository = new ConfigRepository(new ConfigData());

        EDIT_SERVICE = new ConfigEditService(repository, fileStore, normalizer);
        reload();
    }

    public static ConfigSnapshot getConfigSnapshot()
    {
        return service().snapshot();
    }

    public static Object getEffectiveValue(ConfigFieldDescriptor descriptor)
    {
        return service().effectiveValue(descriptor);
    }

    public static ConfigEditResult edit(ConfigEditRequest request)
    {
        return service().edit(request);
    }

    public static synchronized void reload()
    {
        ConfigEditService.ConfigReloadResult result = service().reloadFromFile();
        if (result.repairReport() != null && result.repairReport().hasEntries())
        {
            for (String entry : result.repairReport().entries())
            {
                Constants.LOG.warn("Repaired config: {}", entry);
            }
        }

        if (result.saved())
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
