package com.tik.zbb.config.edit;

import com.tik.zbb.config.schema.ConfigPath;
public record ConfigEditResult(
        boolean success,
        ConfigEditOperation operation,
        ConfigPath path,
        Object effectiveValue,
        int affectedCount,
        String message
)
{
    public static ConfigEditResult success(ConfigEditRequest request, Object effectiveValue, int affectedCount, String message)
    {
        return new ConfigEditResult(true, request.operation(), request.path(), effectiveValue, affectedCount, message);
    }

    public static ConfigEditResult unchanged(ConfigEditRequest request, Object effectiveValue)
    {
        return new ConfigEditResult(true, request.operation(), request.path(), effectiveValue, 0, "updated 0 elements");
    }

    public static ConfigEditResult failure(ConfigEditRequest request, String error)
    {
        return new ConfigEditResult(false, request.operation(), request.path(), null, 0, error);
    }
}
