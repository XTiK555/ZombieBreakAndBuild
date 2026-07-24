package com.tik.zbb.config.schema;

import com.tik.zbb.config.ConfigDocument;
import com.tik.zbb.config.annotations.Range;
import com.tik.zbb.config.annotations.ResourceLocationSemantics;
import com.tik.zbb.utilities.ConfigUtilities;

import java.lang.reflect.Field;
import java.util.List;

public final class ConfigFieldDescriptor
{
    private final ConfigPath path;
    private final List<Field> ownerFields;
    private final Field field;
    private final ConfigValueKind kind;
    private final ConfigValueCodec codec;

    public ConfigFieldDescriptor(ConfigPath path, List<Field> ownerFields, Field field, ConfigValueKind kind)
    {
        this.path = path;
        this.ownerFields = List.copyOf(ownerFields);
        this.field = field;
        this.kind = kind;
        this.codec = kind.codec();
        this.field.setAccessible(true);
    }

    public ConfigPath path()
    {
        return path;
    }

    public ConfigValueKind kind()
    {
        return kind;
    }

    public ConfigValueCodec codec()
    {
        return codec;
    }

    public Range range()
    {
        return field.getAnnotation(Range.class);
    }

    public ResourceLocationSemantics resourceLocationSemantics()
    {
        return field.getAnnotation(ResourceLocationSemantics.class);
    }

    public Object getValue(Object root)
    {
        try
        {
            return field.get(getOwner(root));
        }
        catch (IllegalAccessException e)
        {
            throw new IllegalStateException("Failed to read config path " + path, e);
        }
    }

    public void setValue(Object root, Object value)
    {
        try
        {
            field.set(getOwner(root), copyValue(value));
        }
        catch (IllegalAccessException e)
        {
            throw new IllegalStateException("Failed to write config path " + path, e);
        }
    }

    public Object copyValue(Object value)
    {
        return ConfigUtilities.deepCopyConfigValue(value);
    }

    public Object defaultValue()
    {
        return copyValue(getValue(new ConfigDocument()));
    }

    private Object getOwner(Object root) throws IllegalAccessException
    {
        Object owner = root;
        for (Field ownerField : ownerFields)
        {
            ownerField.setAccessible(true);
            owner = ownerField.get(owner);
        }
        return owner;
    }
}
