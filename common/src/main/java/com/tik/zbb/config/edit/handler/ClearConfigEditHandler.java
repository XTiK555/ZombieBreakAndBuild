package com.tik.zbb.config.edit.handler;

import com.tik.zbb.config.edit.ConfigEditContext;
import com.tik.zbb.config.edit.ConfigEditOperation;
import com.tik.zbb.config.edit.ConfigEditRequest;
import com.tik.zbb.config.edit.ConfigEditResult;
import com.tik.zbb.config.schema.ConfigFieldDescriptor;

public final class ClearConfigEditHandler implements ConfigEditHandler
{
    @Override
    public ConfigEditOperation operation()
    {
        return ConfigEditOperation.CLEAR;
    }

    @Override
    public ConfigEditResult handle(ConfigEditRequest request, ConfigEditContext context)
    {
        ConfigFieldDescriptor descriptor = context.findListDescriptor(request);
        if (descriptor == null) return ConfigEditResult.failure(request, request.path() + " is not a list");

        return context.applyValue(request, descriptor, new java.util.ArrayList<String>());
    }
}
