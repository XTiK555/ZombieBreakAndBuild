package com.tik.zbb.config;

import com.electronwill.nightconfig.core.CommentedConfig;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Map;


public final class ConfigComments
{
    public static void apply(CommentedConfig config, Object root)
    {
        apply(config, root, "");
    }

    private static void apply(CommentedConfig config, Object obj, String path)
    {
        for (Field field : obj.getClass().getFields())
        {
            if (Modifier.isStatic(field.getModifiers())) continue;
            if (Modifier.isTransient(field.getModifiers())) continue;

            String name = field.getName();
            String fullPath = path.isEmpty() ? name : path + "." + name;

            Comment comment = field.getAnnotation(Comment.class);
            if (comment != null)
            {
                config.setComment(fullPath, " " + comment.value());
            }

            if (!isCategory(field.getType())) continue;

            try
            {
                Object nested = field.get(obj);
                if (nested != null)
                {
                    apply(config, nested, fullPath);
                }
            }
            catch (IllegalAccessException ignored)
            {
            }
        }
    }

    private static boolean isCategory(Class<?> type)
    {
        if (type.isPrimitive()) return false;
        if (type == String.class) return false;
        if (Number.class.isAssignableFrom(type)) return false;
        if (type == Boolean.class) return false;
        if (type.isEnum()) return false;
        if (Collection.class.isAssignableFrom(type)) return false;
        if (Map.class.isAssignableFrom(type)) return false;

        return true;
    }
}
