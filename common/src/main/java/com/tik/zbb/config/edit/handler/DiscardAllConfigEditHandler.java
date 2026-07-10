package com.tik.zbb.config.edit.handler;

import com.tik.zbb.config.edit.ConfigEditContext;
import com.tik.zbb.config.edit.ConfigEditOperation;
import com.tik.zbb.config.edit.ConfigEditRequest;
import com.tik.zbb.config.edit.ConfigEditResult;

public final class DiscardAllConfigEditHandler implements ConfigEditHandler
{
    @Override
    public ConfigEditOperation operation()
    {
        return ConfigEditOperation.DISCARD_ALL;
    }

    @Override
    public ConfigEditResult handle(ConfigEditRequest request, ConfigEditContext context)
    {
        int count = context.repository().discardAll();
        return ConfigEditResult.success(request, null, false, count, "discarded " + count + " temporary value(s)");
    }
}
