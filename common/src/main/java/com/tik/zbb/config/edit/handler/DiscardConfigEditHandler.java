package com.tik.zbb.config.edit.handler;

import com.tik.zbb.config.edit.ConfigEditContext;
import com.tik.zbb.config.edit.ConfigEditOperation;
import com.tik.zbb.config.edit.ConfigEditRequest;
import com.tik.zbb.config.edit.ConfigEditResult;
import com.tik.zbb.config.schema.ConfigSchema;

public final class DiscardConfigEditHandler implements ConfigEditHandler
{
    @Override
    public ConfigEditOperation operation()
    {
        return ConfigEditOperation.DISCARD;
    }

    @Override
    public ConfigEditResult handle(ConfigEditRequest request, ConfigEditContext context)
    {
        if (!ConfigSchema.hasPathOrSection(request.path()))
        {
            return ConfigEditResult.failure(request, "Unknown config path or section: " + request.path());
        }

        int count = context.repository().discard(request.path());
        return ConfigEditResult.success(request, null, false, count, "discarded " + count + " temporary value(s)");
    }
}
