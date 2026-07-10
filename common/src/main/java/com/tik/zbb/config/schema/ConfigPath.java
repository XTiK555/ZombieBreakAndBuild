package com.tik.zbb.config.schema;

import java.util.Objects;

public record ConfigPath(String value)
{
    public ConfigPath
    {
        value = Objects.requireNonNull(value, "value").trim();
        if (value.isEmpty()) throw new IllegalArgumentException("Config path cannot be empty");
    }

    public boolean isRoot()
    {
        return value.indexOf('.') < 0;
    }

    public ConfigPath parent()
    {
        int lastDot = value.lastIndexOf('.');
        if (lastDot < 0) return null;
        return new ConfigPath(value.substring(0, lastDot));
    }

    public boolean isDescendantOf(ConfigPath parent)
    {
        return value.equals(parent.value()) || value.startsWith(parent.value() + ".");
    }

    @Override
    public String toString()
    {
        return value;
    }
}
