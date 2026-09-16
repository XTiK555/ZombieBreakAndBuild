package com.tik.zbb.config.edit;

import com.tik.zbb.config.schema.ConfigPath;

public record ConfigEditRequest(
        ConfigEditOperation operation,
        ConfigPath path,
        Object value
)
{
    public ConfigEditRequest
    {
        if (operation == null) throw new IllegalArgumentException("operation is required");
        switch (operation)
        {
            case SET, ADD, REMOVE ->
            {
                if (path == null) throw new IllegalArgumentException(operation + " requires path");
                if (value == null) throw new IllegalArgumentException(operation + " requires value");
            }
            case CLEAR, RESET_TO_DEFAULT ->
            {
                if (path == null) throw new IllegalArgumentException(operation + " requires path");
                if (value != null) throw new IllegalArgumentException(operation + " does not accept value");
            }
            case RESET_ALL_TO_DEFAULTS ->
            {
                if (path != null) throw new IllegalArgumentException(operation + " does not accept path");
                if (value != null) throw new IllegalArgumentException(operation + " does not accept value");
            }
        }
    }

    public ConfigEditRequest withValue(Object newValue)
    {
        return new ConfigEditRequest(operation, path, newValue);
    }

    public static ConfigEditRequest set(ConfigPath path, Object value)
    {
        return new ConfigEditRequest(ConfigEditOperation.SET, path, value);
    }

    public static ConfigEditRequest add(ConfigPath path, Object value)
    {
        return new ConfigEditRequest(ConfigEditOperation.ADD, path, value);
    }

    public static ConfigEditRequest remove(ConfigPath path, Object value)
    {
        return new ConfigEditRequest(ConfigEditOperation.REMOVE, path, value);
    }

    public static ConfigEditRequest clear(ConfigPath path)
    {
        return new ConfigEditRequest(ConfigEditOperation.CLEAR, path, null);
    }

    public static ConfigEditRequest reset(ConfigPath path)
    {
        return new ConfigEditRequest(ConfigEditOperation.RESET_TO_DEFAULT, path, null);
    }

    public static ConfigEditRequest resetAll()
    {
        return new ConfigEditRequest(ConfigEditOperation.RESET_ALL_TO_DEFAULTS, null, null);
    }
}
