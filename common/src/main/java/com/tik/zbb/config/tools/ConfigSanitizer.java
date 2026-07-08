package com.tik.zbb.config.tools;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.serde.ObjectSerializer;
import com.tik.zbb.config.annotations.Range;
import com.tik.zbb.config.annotations.ResourceLocationList;
import com.tik.zbb.config.annotations.ResourceLocationPairList;
import com.tik.zbb.config.annotations.ResourceLocationString;
import com.tik.zbb.utilities.ConfigUtilities;
import net.minecraft.resources.Identifier;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public final class ConfigSanitizer
{
    public static void sanitize(CommentedConfig target, Object defaults, ObjectSerializer serializer)
    {
        CommentedConfig defaultConfig = CommentedConfig.inMemory();
        serializer.serializeFields(defaults, defaultConfig);

        sanitizeObject(target, defaultConfig, defaults.getClass());
    }

    private static void sanitizeObject(CommentedConfig target, UnmodifiableConfig defaultConfig, Class<?> type)
    {
        for (Field field : ConfigUtilities.getConfigFields(type))
        {
            String key = field.getName();

            Object defaultValue = defaultConfig.getRaw(key);
            Object value = target.getRaw(key);

            if (ConfigUtilities.isNestedConfigField(field))
            {
                Object defaultNested = defaultConfig.getRaw(key);

                if (!(value instanceof CommentedConfig targetNested) || !(defaultNested instanceof UnmodifiableConfig defaultNestedConfig))
                {
                    target.set(key, deepCopyRaw(defaultNested));
                    continue;
                }

                sanitizeObject(targetNested, defaultNestedConfig, field.getType());
                continue;
            }

            Object fixed = sanitizeValue(field, value, defaultValue);
            target.set(key, fixed);
        }
    }

    private static Object sanitizeValue(Field field, Object value, Object defaultValue)
    {
        if (value == null)
        {
            return deepCopyRaw(defaultValue);
        }

        Class<?> fieldType = field.getType();

        if (fieldType == boolean.class || fieldType == Boolean.class)
        {
            return value instanceof Boolean ? value : defaultValue;
        }

        if (fieldType == int.class || fieldType == Integer.class)
        {
            if (!(value instanceof Number n)) return defaultValue;

            Range range = field.getAnnotation(Range.class);
            int i = n.intValue();
            if (range != null && (i < range.min() || i > range.max())) return defaultValue;

            return i;
        }

        if (fieldType == long.class || fieldType == Long.class)
        {
            if (!(value instanceof Number n)) return defaultValue;

            Range range = field.getAnnotation(Range.class);
            long l = n.longValue();
            if (range != null && (l < range.min() || l > range.max())) return defaultValue;

            return l;
        }

        if (fieldType == double.class || fieldType == Double.class)
        {
            if (!(value instanceof Number n)) return defaultValue;

            double d = n.doubleValue();
            Range range = field.getAnnotation(Range.class);
            if (range != null && (d < range.min() || d > range.max())) return defaultValue;

            return d;
        }

        if (fieldType == float.class || fieldType == Float.class)
        {
            if (!(value instanceof Number n)) return defaultValue;

            float f = n.floatValue();
            Range range = field.getAnnotation(Range.class);
            if (range != null && (f < range.min() || f > range.max())) return defaultValue;

            return f;
        }

        if (fieldType == String.class)
        {
            if (!(value instanceof String s)) return defaultValue;

            if (field.isAnnotationPresent(ResourceLocationString.class))
            {
                return Identifier.tryParse(s) != null ? s : defaultValue;
            }

            return s;
        }

        if (field.isAnnotationPresent(ResourceLocationList.class))
        {
            if (!(value instanceof List<?> list)) return deepCopyRaw(defaultValue);

            List<String> cleaned = new ArrayList<>();
            for (Object o : list)
            {
                if (o instanceof String s && Identifier.tryParse(s) != null)
                {
                    cleaned.add(s);
                }
            }
            return cleaned;
        }

        if (field.isAnnotationPresent(ResourceLocationPairList.class))
        {
            if (!(value instanceof List<?> list)) return deepCopyRaw(defaultValue);

            List<String> cleaned = new ArrayList<>();
            for (Object o : list)
            {
                if (!(o instanceof String s)) continue;

                String[] parts = s.split("=", 2);
                if (parts.length != 2) continue;

                Identifier key = Identifier.tryParse(parts[0].trim());
                Identifier pairValue = Identifier.tryParse(parts[1].trim());
                if (key != null && pairValue != null)
                {
                    cleaned.add(key + "=" + pairValue);
                }
            }
            return cleaned;
        }

        return value;
    }

    private static Object deepCopyRaw(Object value)
    {
        if (value instanceof List<?> list)
        {
            return new ArrayList<>(list);
        }
        return value;
    }
}
