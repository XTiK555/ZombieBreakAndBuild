package com.tik.zbb.config.io.format;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.tik.zbb.config.annotations.Comment;
import com.tik.zbb.utilities.ConfigUtilities;

import java.lang.reflect.Field;


public final class ConfigComments
{
    public static void apply(CommentedConfig config, Object root)
    {
        apply(config, root, "");
    }

    private static void apply(CommentedConfig config, Object obj, String path)
    {
        for (Field field : ConfigUtilities.getConfigFields(obj.getClass()))
        {
            String name = field.getName();
            String fullPath = path.isEmpty() ? name : path + "." + name;

            Comment comment = field.getAnnotation(Comment.class);
            if (comment != null)
            {
                config.setComment(fullPath, " " + comment.value());
            }

            if (!ConfigUtilities.isNestedConfigField(field)) continue;

            try
            {
                Object nested = field.get(obj);
                ConfigUtilities.forEachNestedConfigValue(nested, value -> apply(config, value, fullPath));
            }
            catch (IllegalAccessException ignored)
            {
            }
        }
    }
}
