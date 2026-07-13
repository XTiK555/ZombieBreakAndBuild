package com.tik.zbb.config.edit;

import com.tik.zbb.config.schema.ConfigPath;

public record ConfigEditRequest(
        ConfigEditOperation operation,
        ConfigPath path,
        Object value,
        ConfigWriteMode writeMode
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
                if (writeMode == null) throw new IllegalArgumentException(operation + " requires write mode");
            }
            case CLEAR, RESET_TO_DEFAULT ->
            {
                if (path == null) throw new IllegalArgumentException(operation + " requires path");
                if (value != null) throw new IllegalArgumentException(operation + " does not accept value");
                if (writeMode == null) throw new IllegalArgumentException(operation + " requires write mode");
            }
            case RESET_ALL_TO_DEFAULTS ->
            {
                if (path != null) throw new IllegalArgumentException(operation + " does not accept path");
                if (value != null) throw new IllegalArgumentException(operation + " does not accept value");
                if (writeMode == null) throw new IllegalArgumentException(operation + " requires write mode");
            }
            case REVERT_TO_PERSISTED ->
            {
                if (path == null) throw new IllegalArgumentException(operation + " requires path");
                if (value != null) throw new IllegalArgumentException(operation + " does not accept value");
                writeMode = ConfigWriteMode.RUNTIME_ONLY;
            }
            case DISCARD_ALL_OVERRIDES ->
            {
                if (path != null) throw new IllegalArgumentException(operation + " does not accept path");
                if (value != null) throw new IllegalArgumentException(operation + " does not accept value");
                writeMode = ConfigWriteMode.RUNTIME_ONLY;
            }
        }
    }

    public ConfigEditRequest withValue(Object newValue)
    {
        return new ConfigEditRequest(operation, path, newValue, writeMode);
    }

    public static ConfigEditRequest set(ConfigPath path, Object value, ConfigWriteMode writeMode)
    {
        return new ConfigEditRequest(ConfigEditOperation.SET, path, value, writeMode);
    }

    public static ConfigEditRequest add(ConfigPath path, Object value, ConfigWriteMode writeMode)
    {
        return new ConfigEditRequest(ConfigEditOperation.ADD, path, value, writeMode);
    }

    public static ConfigEditRequest remove(ConfigPath path, Object value, ConfigWriteMode writeMode)
    {
        return new ConfigEditRequest(ConfigEditOperation.REMOVE, path, value, writeMode);
    }

    public static ConfigEditRequest clear(ConfigPath path, ConfigWriteMode writeMode)
    {
        return new ConfigEditRequest(ConfigEditOperation.CLEAR, path, null, writeMode);
    }

    public static ConfigEditRequest reset(ConfigPath path, ConfigWriteMode writeMode)
    {
        return new ConfigEditRequest(ConfigEditOperation.RESET_TO_DEFAULT, path, null, writeMode);
    }

    public static ConfigEditRequest resetAll(ConfigWriteMode writeMode)
    {
        return new ConfigEditRequest(ConfigEditOperation.RESET_ALL_TO_DEFAULTS, null, null, writeMode);
    }

    public static ConfigEditRequest discard(ConfigPath path)
    {
        return new ConfigEditRequest(ConfigEditOperation.REVERT_TO_PERSISTED, path, null, ConfigWriteMode.RUNTIME_ONLY);
    }

    public static ConfigEditRequest discardAll()
    {
        return new ConfigEditRequest(ConfigEditOperation.DISCARD_ALL_OVERRIDES, null, null, ConfigWriteMode.RUNTIME_ONLY);
    }
}
