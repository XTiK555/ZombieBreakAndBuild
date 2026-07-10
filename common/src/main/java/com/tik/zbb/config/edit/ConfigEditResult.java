package com.tik.zbb.config.edit;

import com.tik.zbb.config.schema.ConfigPath;
import com.tik.zbb.config.schema.ConfigRepairReport;

import java.util.List;

public record ConfigEditResult(
        boolean success,
        ConfigEditOperation operation,
        ConfigPath path,
        Object effectiveValue,
        ConfigWriteMode writeMode,
        boolean persisted,
        int affectedCount,
        String message,
        List<String> errors,
        ConfigRepairReport repairReport
)
{
    public static ConfigEditResult success(ConfigEditRequest request, Object effectiveValue, boolean persisted, int affectedCount, String message)
    {
        return new ConfigEditResult(true, request.operation(), request.path(), effectiveValue, request.writeMode(), persisted, affectedCount, message, List.of(), null);
    }

    public static ConfigEditResult failure(ConfigEditRequest request, String error)
    {
        return new ConfigEditResult(false, request.operation(), request.path(), null, request.writeMode(), false, 0, error, List.of(error), null);
    }
}
