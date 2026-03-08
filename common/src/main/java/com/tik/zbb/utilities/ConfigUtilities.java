package com.tik.zbb.utilities;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class ConfigUtilities
{
    public static List<Field> getConfigFields(Class<?> type)
    {
        List<Field> result = new ArrayList<>();
        for (Field field : type.getDeclaredFields())
        {
            if (Modifier.isStatic(field.getModifiers())) continue;
            if (Modifier.isTransient(field.getModifiers())) continue;
            field.setAccessible(true);
            result.add(field);
        }
        return result;
    }

    public static boolean isNestedConfigField(Field field)
    {
        return !isSimpleValueType(field.getType());
    }

    public static boolean isSimpleValueType(Class<?> type)
    {
        if (type.isPrimitive()) return true;
        if (type == String.class) return true;
        if (Number.class.isAssignableFrom(type)) return true;
        if (type == Boolean.class) return true;
        if (type.isEnum()) return true;
        if (Collection.class.isAssignableFrom(type)) return true;
        if (Map.class.isAssignableFrom(type)) return true;
        return false;
    }
}
