package com.tik.zbb.config.edit.handler;

import com.tik.zbb.config.ConfigData;
import com.tik.zbb.config.edit.ConfigEditContext;
import com.tik.zbb.config.edit.ConfigEditOperation;
import com.tik.zbb.config.edit.ConfigEditRequest;
import com.tik.zbb.config.edit.ConfigEditResult;
import com.tik.zbb.config.schema.ConfigFieldDescriptor;

public final class AddConfigEditHandler implements ConfigEditHandler
{
    @Override
    public ConfigEditOperation operation()
    {
        return ConfigEditOperation.ADD;
    }

    @Override
    public ConfigEditResult handle(ConfigEditRequest request, ConfigEditContext context)
    {
        ConfigFieldDescriptor descriptor = context.findListDescriptor(request);
        if (descriptor == null) return ConfigEditResult.failure(request, request.path() + " is not a list");

        try
        {
            if (!(request.value() instanceof String entry))
            {
                return ConfigEditResult.failure(request, descriptor.path() + ": Expected string list entry");
            }

            ConfigData base = context.baseData(request.writeMode());
            @SuppressWarnings("unchecked")
            java.util.List<String> values = new java.util.ArrayList<>((java.util.List<String>) descriptor.getValue(base));
            values.add(entry);
            return context.applyValue(request, descriptor, values);
        }
        catch (ClassCastException e)
        {
            return ConfigEditResult.failure(request, descriptor.path() + ": Expected string list");
        }
    }
}
