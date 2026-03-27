package com.tik.zbb.platform;

import com.tik.zbb.platform.services.IPlatformHelper;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

public class ForgePlatformHelper implements IPlatformHelper
{
    @Override
    public String getPlatformName()
    {
        return "Forge";
    }

    @Override
    public boolean isModLoaded(String modId)
    {
        return ModList.isLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment()
    {
        return !FMLLoader.isProduction();
    }

    @Override
    public Path getConfigDir()
    {
        return FMLPaths.CONFIGDIR.get();
    }
}