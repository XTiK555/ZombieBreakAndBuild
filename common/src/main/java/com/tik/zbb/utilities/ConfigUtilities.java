package com.tik.zbb.utilities;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public final class ConfigUtilities
{
    public static List<Field> getConfigFields(Class<?> type)
    {
        List<Class<?>> hierarchy = new ArrayList<>();
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass())
        {
            hierarchy.add(current);
        }
        Collections.reverse(hierarchy);

        Map<String, Field> fieldsByName = new LinkedHashMap<>();
        for (Class<?> current : hierarchy)
        {
            for (Field field : current.getDeclaredFields())
            {
                if (Modifier.isStatic(field.getModifiers())) continue;
                if (Modifier.isTransient(field.getModifiers())) continue;
                if (field.isSynthetic()) continue;
                field.setAccessible(true);
                fieldsByName.put(field.getName(), field);
            }
        }
        return List.copyOf(fieldsByName.values());
    }

    public static boolean isNestedConfigField(Field field)
    {
        return !isSimpleValueType(field.getGenericType());
    }

    public static boolean isConfigSectionField(Field field)
    {
        Class<?> type = field.getType();
        return !type.isArray()
                && !Collection.class.isAssignableFrom(type)
                && !Map.class.isAssignableFrom(type)
                && !isSimpleValueType(type);
    }

    public static boolean isSimpleValueType(Class<?> type)
    {
        if (type.isPrimitive()) return true;
        if (type == String.class) return true;
        if (Number.class.isAssignableFrom(type)) return true;
        if (type == Boolean.class) return true;
        return type.isEnum();
    }

    public static Object deepCopyConfigValue(Object value)
    {
        return deepCopyConfigValue(value, new IdentityHashMap<>());
    }

    private static Object deepCopyConfigValue(Object value, IdentityHashMap<Object, Object> copies)
    {
        if (value == null || isSimpleValueType(value.getClass())) return value;
        Object existingCopy = copies.get(value);
        if (existingCopy != null) return existingCopy;

        if (value.getClass().isArray())
        {
            int length = Array.getLength(value);
            Object copy = Array.newInstance(value.getClass().getComponentType(), length);
            copies.put(value, copy);
            for (int index = 0; index < length; index++)
            {
                Array.set(copy, index, deepCopyConfigValue(Array.get(value, index), copies));
            }
            return copy;
        }
        if (value instanceof List<?> list)
        {
            List<Object> copy = new ArrayList<>(list.size());
            copies.put(value, copy);
            for (Object element : list) copy.add(deepCopyConfigValue(element, copies));
            return copy;
        }
        if (value instanceof Set<?> set)
        {
            Set<Object> copy = new LinkedHashSet<>();
            copies.put(value, copy);
            for (Object element : set) copy.add(deepCopyConfigValue(element, copies));
            return copy;
        }
        if (value instanceof Collection<?> collection)
        {
            List<Object> copy = new ArrayList<>(collection.size());
            copies.put(value, copy);
            for (Object element : collection) copy.add(deepCopyConfigValue(element, copies));
            return copy;
        }
        if (value instanceof Map<?, ?> map)
        {
            Map<Object, Object> copy = new LinkedHashMap<>();
            copies.put(value, copy);
            for (Map.Entry<?, ?> entry : map.entrySet())
            {
                copy.put(deepCopyConfigValue(entry.getKey(), copies), deepCopyConfigValue(entry.getValue(), copies));
            }
            return copy;
        }

        try
        {
            var constructor = value.getClass().getDeclaredConstructor();
            constructor.setAccessible(true);
            Object copy = constructor.newInstance();
            copies.put(value, copy);
            for (Field field : getConfigFields(value.getClass()))
            {
                field.set(copy, deepCopyConfigValue(field.get(value), copies));
            }
            return copy;
        }
        catch (ReflectiveOperationException e)
        {
            throw new IllegalStateException("Failed to copy config value of type " + value.getClass().getName(), e);
        }
    }

    public static void forEachNestedConfigValue(Object value, Consumer<Object> consumer)
    {
        if (value == null || isSimpleValueType(value.getClass())) return;
        if (value instanceof Map<?, ?> map)
        {
            for (Object nested : map.values()) forEachNestedConfigValue(nested, consumer);
            return;
        }
        if (value instanceof Collection<?> collection)
        {
            for (Object nested : collection) forEachNestedConfigValue(nested, consumer);
            return;
        }
        if (value.getClass().isArray())
        {
            for (int index = 0; index < Array.getLength(value); index++)
            {
                forEachNestedConfigValue(Array.get(value, index), consumer);
            }
            return;
        }
        consumer.accept(value);
    }

    private static boolean isSimpleValueType(Type type)
    {
        if (type instanceof Class<?> valueClass)
        {
            if (valueClass.isArray()) return isSimpleValueType(valueClass.getComponentType());
            return isSimpleValueType(valueClass);
        }
        if (type instanceof GenericArrayType arrayType)
        {
            return isSimpleValueType(arrayType.getGenericComponentType());
        }
        if (type instanceof WildcardType wildcardType)
        {
            for (Type upperBound : wildcardType.getUpperBounds())
            {
                if (!isSimpleValueType(upperBound)) return false;
            }
            return wildcardType.getLowerBounds().length == 0;
        }
        if (type instanceof ParameterizedType parameterizedType
                && parameterizedType.getRawType() instanceof Class<?> rawType)
        {
            Type[] arguments = parameterizedType.getActualTypeArguments();
            if (Collection.class.isAssignableFrom(rawType))
            {
                return arguments.length == 1 && isSimpleValueType(arguments[0]);
            }
            if (Map.class.isAssignableFrom(rawType))
            {
                return arguments.length == 2
                        && isSimpleValueType(arguments[0])
                        && isSimpleValueType(arguments[1]);
            }
            return isSimpleValueType(rawType);
        }
        return false;
    }

    private ConfigUtilities() {}
}
