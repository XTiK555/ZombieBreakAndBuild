package com.tik.zbb.config.edit.handler;

import com.tik.zbb.config.edit.ConfigEditContext;
import com.tik.zbb.config.edit.ConfigEditOperation;
import com.tik.zbb.config.edit.ConfigEditRequest;
import com.tik.zbb.config.edit.ConfigEditResult;

public interface ConfigEditHandler
{
    ConfigEditOperation operation();

    ConfigEditResult handle(ConfigEditRequest request, ConfigEditContext context);
}
