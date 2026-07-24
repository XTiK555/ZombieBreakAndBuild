package com.tik.zbb.config.edit;

import com.tik.zbb.config.schema.ConfigPath;
public record ConfigEditResult(
        boolean success,
        ConfigEditOperation operation,
        ConfigPath path,
        Object effectiveValue,
        ConfigWriteMode writeMode,
        boolean persisted,
        int affectedCount,
        String message
)
{
    public static ConfigEditResult success(ConfigEditRequest request, Object effectiveValue, boolean persisted, int affectedCount, String message)
    {
        return new ConfigEditResult(true, request.operation(), request.path(), effectiveValue, request.writeMode(), persisted, affectedCount, message);
    }

    public static ConfigEditResult unchanged(ConfigEditRequest request, Object effectiveValue, boolean persisted)
    {
        return new ConfigEditResult(false, request.operation(), request.path(), effectiveValue, request.writeMode(), persisted, 0, "unchanged");
    }

    public static ConfigEditResult failure(ConfigEditRequest request, String error)
    {
        return new ConfigEditResult(false, request.operation(), request.path(), null, request.writeMode(), false, 0, error);
    }
}
