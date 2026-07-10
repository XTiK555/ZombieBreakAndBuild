package com.tik.zbb.config.edit.handler;

import com.tik.zbb.config.edit.ConfigEditContext;
import com.tik.zbb.config.edit.ConfigEditOperation;
import com.tik.zbb.config.edit.ConfigEditRequest;
import com.tik.zbb.config.edit.ConfigEditResult;
import com.tik.zbb.config.schema.ConfigFieldDescriptor;

public final class SetConfigEditHandler implements ConfigEditHandler
{
    @Override
    public ConfigEditOperation operation()
    {
        return ConfigEditOperation.SET;
    }

    @Override
    public ConfigEditResult handle(ConfigEditRequest request, ConfigEditContext context)
    {
        ConfigFieldDescriptor descriptor = context.findDescriptor(request);
        if (descriptor == null) return ConfigEditResult.failure(request, "Unknown config path: " + request.path());

        return context.applyValue(request, descriptor, request.value());
    }
}
