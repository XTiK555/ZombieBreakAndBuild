package com.tik.zbb.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.tik.zbb.Constants;
import com.tik.zbb.config.tools.ConfigComments;
import com.tik.zbb.config.tools.ConfigFormatter;
import com.tik.zbb.config.tools.ConfigSanitizer;
import com.tik.zbb.platform.Services;
import com.tik.zbb.utilities.ConfigUtilities;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ConfigManager
{
    private static volatile CommentedFileConfig FILE_CONFIG;
    private static volatile ConfigData DATA = new ConfigData();
    private static volatile long VERSION = 0;

    public static void init()
    {
        Config.setInsertionOrderPreserved(true);

        String modName = Constants.MOD_NAME.replaceAll("\\s", "-").toLowerCase();
        Path configPath = Services.PLATFORM.getConfigDir().resolve(modName + ".toml");

        FILE_CONFIG = CommentedFileConfig.builder(configPath).sync().build();

        reload();
    }

    public static ConfigSnapshot getConfigSnapshot()
    {
        return new ConfigSnapshot(DATA, VERSION);
    }

    public static synchronized void reload()
    {
        if (FILE_CONFIG == null)
        {
            Constants.LOG.warn("Config reload requested before init");
            return;
        }

        ConfigData defaults = new ConfigData();

        try
        {
            FILE_CONFIG.load();
        }
        catch (Exception e)
        {
            Constants.LOG.error("Failed to parse config, restoring defaults", e);
            setData(defaults);
            save();
            return;
        }

        try
        {
            ConfigSanitizer.sanitize(FILE_CONFIG, defaults);

            ConfigData loaded = readObject(FILE_CONFIG, ConfigData.class);

            setData(loaded);
            save();

            Constants.LOG.info("Reloaded config");
        }
        catch (Exception e)
        {
            Constants.LOG.error("Failed to sanitize/deserialize config, restoring defaults", e);
            setData(defaults);
            save();
        }
    }

    private static void save()
    {
        try
        {
            CommentedConfig config = CommentedConfig.inMemory();

            writeObject(DATA, config);
            ConfigComments.apply(config, DATA);

            FILE_CONFIG.clear();
            FILE_CONFIG.putAll(config);
            FILE_CONFIG.save();
        }
        catch (Exception e)
        {
            Constants.LOG.error("Failed to save config", e);
            return;
        }

        try
        {
            ConfigFormatter.addHeader(FILE_CONFIG.getNioPath());
            ConfigFormatter.splitBlocks(FILE_CONFIG.getNioPath());
        }
        catch (Exception e)
        {
            Constants.LOG.warn("Failed to format config file", e);
        }
    }

    private static void setData(ConfigData data)
    {
        data.rebuildSets();
        DATA = data;
        VERSION++;
    }

    private static <T> T readObject(CommentedConfig config, Class<T> type)
    {
        try
        {
            T instance = type.getDeclaredConstructor().newInstance();

            for (Field field : ConfigUtilities.getConfigFields(type))
            {
                String key = field.getName();

                if (ConfigUtilities.isNestedConfigField(field))
                {
                    Object rawNested = config.getRaw(key);
                    if (rawNested instanceof CommentedConfig nestedConfig)
                    {
                        Object nestedObject = readObject(nestedConfig, field.getType());
                        field.set(instance, nestedObject);
                    }
                    continue;
                }

                Object rawValue = config.getRaw(key);
                Object converted = convertLoadedValue(rawValue, field.getType());
                if (converted != null)
                {
                    field.set(instance, converted);
                }
            }

            return instance;
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to read config object: " + type.getName(), e);
        }
    }


    private static void writeObject(Object source, CommentedConfig target)
    {
        try
        {
            for (Field field : ConfigUtilities.getConfigFields(source.getClass()))
            {
                String key = field.getName();
                Object value = field.get(source);

                if (value == null) continue;

                if (ConfigUtilities.isNestedConfigField(field))
                {
                    CommentedConfig nested = CommentedConfig.inMemory();
                    writeObject(value, nested);
                    target.set(key, nested);
                    continue;
                }

                if (value instanceof List<?> list)
                {
                    target.set(key, new ArrayList<>(list));
                }
                else
                {
                    target.set(key, value);
                }
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to write config object: " + source.getClass().getName(), e);
        }
    }

    private static Object convertLoadedValue(Object value, Class<?> targetType)
    {
        if (value == null) return null;

        if (targetType == boolean.class || targetType == Boolean.class)
        {
            return value instanceof Boolean b ? b : null;
        }

        if (targetType == int.class || targetType == Integer.class)
        {
            return value instanceof Number n ? n.intValue() : null;
        }

        if (targetType == long.class || targetType == Long.class)
        {
            return value instanceof Number n ? n.longValue() : null;
        }

        if (targetType == double.class || targetType == Double.class)
        {
            return value instanceof Number n ? n.doubleValue() : null;
        }

        if (targetType == float.class || targetType == Float.class)
        {
            return value instanceof Number n ? n.floatValue() : null;
        }

        if (targetType == String.class)
        {
            return value instanceof String s ? s : null;
        }

        if (List.class.isAssignableFrom(targetType))
        {
            if (!(value instanceof List<?> list)) return null;

            List<String> out = new ArrayList<>();
            for (Object element : list)
            {
                if (element instanceof String s)
                {
                    out.add(s);
                }
            }
            return out;
        }

        return value;
    }
}
