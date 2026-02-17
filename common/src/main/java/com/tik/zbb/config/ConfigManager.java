package com.tik.zbb.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tik.zbb.Constants;
import com.tik.zbb.platform.Services;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigManager
{
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static ConfigData DATA = new ConfigData();
    private static Path CONFIG_PATH;

    public static void init(String configFileName)
    {
        CONFIG_PATH = Services.PLATFORM.getConfigDir().resolve(configFileName);
        loadOrCreate();
    }

    public static ConfigData getConfigData()
    {
        return DATA;
    }

    public static void reload()
    {
        loadOrCreate();
    }

    public static void save()
    {
        try
        {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (BufferedWriter w = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8))
            {
                GSON.toJson(DATA, w);
            }
        }
        catch (Exception e)
        {
            Constants.LOG.error("[ZBB] Failed to save config: " + e);
        }
    }

    private static void loadOrCreate()
    {
        ConfigData loadedData = null;

        if (Files.exists(CONFIG_PATH))
        {
            try (BufferedReader bufferedReader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8))
            {
                loadedData = GSON.fromJson(bufferedReader, ConfigData.class);
            }
            catch (Exception e)
            {
                Constants.LOG.error("[ZBB] Failed to read config, using defaults: " + e);
            }
        }

        if (loadedData == null)
        {
            loadedData = new ConfigData();
        }

        DATA = validateNullFields(loadedData, new ConfigData());
        DATA.rebuildSets();
        save();
    }

    private static ConfigData validateNullFields(ConfigData loaded, ConfigData def)
    {
        if (loaded.bridgeBlockId == null) loaded.bridgeBlockId = def.bridgeBlockId;
        if (loaded.dangerousBlockIdList == null) loaded.dangerousBlockIdList = def.dangerousBlockIdList;
        if (loaded.impassableBlockIdList == null) loaded.impassableBlockIdList = def.impassableBlockIdList;
        if (loaded.isAlwaysSeeNearestPlayer == null) loaded.isAlwaysSeeNearestPlayer = def.isAlwaysSeeNearestPlayer;
        if (loaded.isApplyingToAllHostiles == null) loaded.isApplyingToAllHostiles = def.isApplyingToAllHostiles;
        if (loaded.isAttackingAllEntities == null) loaded.isAttackingAllEntities = def.isAttackingAllEntities;
        if (loaded.targetSearchRadius == null) loaded.targetSearchRadius = def.targetSearchRadius;
        if (loaded.dangerousBlocksSearchRadius == null)
            loaded.dangerousBlocksSearchRadius = def.dangerousBlocksSearchRadius;
        if (loaded.breakCooldown == null) loaded.breakCooldown = def.breakCooldown;
        if (loaded.buildCooldown == null) loaded.buildCooldown = def.buildCooldown;
        if (loaded.damageToBlocks == null) loaded.damageToBlocks = def.damageToBlocks;
        if (loaded.freezeTime == null) loaded.freezeTime = def.freezeTime;
        if (loaded.searchDangerousBlocksInterval == null)
            loaded.searchDangerousBlocksInterval = def.searchDangerousBlocksInterval;
        if (loaded.goToTargetInterval == null) loaded.goToTargetInterval = def.goToTargetInterval;
        if (loaded.pathCheckInterval == null) loaded.pathCheckInterval = def.pathCheckInterval;
        if (loaded.stuckSecondsBeforeBreakAndBuild == null)
            loaded.stuckSecondsBeforeBreakAndBuild = def.stuckSecondsBeforeBreakAndBuild;
        if (loaded.damageStoreTime == null) loaded.damageStoreTime = def.damageStoreTime;
        if (loaded.builtBlocksProtectionTime == null) loaded.builtBlocksProtectionTime = def.builtBlocksProtectionTime;
        if (loaded.placeSoundId == null) loaded.placeSoundId = def.placeSoundId;
        if (loaded.hitSoundId == null) loaded.hitSoundId = def.hitSoundId;
        if (loaded.breakSoundId == null) loaded.breakSoundId = def.breakSoundId;

        return loaded;
    }
}
