package com.tik.zbb.config.edit;

import com.tik.zbb.config.schema.ConfigPath;

public record ConfigEditRequest(
        ConfigEditOperation operation,
        ConfigPath path,
        Object value,
        ConfigWriteMode writeMode,
        String source
)
{
    public static ConfigEditRequest set(ConfigPath path, Object value, ConfigWriteMode writeMode, String source)
    {
        return new ConfigEditRequest(ConfigEditOperation.SET, path, value, writeMode, source);
    }

    public static ConfigEditRequest add(ConfigPath path, Object value, ConfigWriteMode writeMode, String source)
    {
        return new ConfigEditRequest(ConfigEditOperation.ADD, path, value, writeMode, source);
    }

    public static ConfigEditRequest remove(ConfigPath path, Object value, ConfigWriteMode writeMode, String source)
    {
        return new ConfigEditRequest(ConfigEditOperation.REMOVE, path, value, writeMode, source);
    }

    public static ConfigEditRequest clear(ConfigPath path, ConfigWriteMode writeMode, String source)
    {
        return new ConfigEditRequest(ConfigEditOperation.CLEAR, path, null, writeMode, source);
    }

    public static ConfigEditRequest reset(ConfigPath path, ConfigWriteMode writeMode, String source)
    {
        return new ConfigEditRequest(ConfigEditOperation.RESET, path, null, writeMode, source);
    }

    public static ConfigEditRequest resetAll(ConfigWriteMode writeMode, String source)
    {
        return new ConfigEditRequest(ConfigEditOperation.RESET_ALL, null, null, writeMode, source);
    }

    public static ConfigEditRequest discard(ConfigPath path, String source)
    {
        return new ConfigEditRequest(ConfigEditOperation.DISCARD, path, null, ConfigWriteMode.RUNTIME_ONLY, source);
    }

    public static ConfigEditRequest discardAll(String source)
    {
        return new ConfigEditRequest(ConfigEditOperation.DISCARD_ALL, null, null, ConfigWriteMode.RUNTIME_ONLY, source);
    }
}
