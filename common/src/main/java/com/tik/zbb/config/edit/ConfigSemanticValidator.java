package com.tik.zbb.config.edit;

import com.tik.zbb.config.schema.ConfigFieldDescriptor;
import com.tik.zbb.config.schema.ConfigRepairReport;
import com.tik.zbb.config.schema.ConfigValidationException;

public interface ConfigSemanticValidator
{
    ConfigSemanticValidator NONE = (descriptor, value) -> {};

    void validate(ConfigFieldDescriptor descriptor, Object value) throws ConfigValidationException;

    default Object repairValue(
            ConfigFieldDescriptor descriptor,
            Object value,
            Object defaultValue,
            ConfigRepairReport report
    )
    {
        try
        {
            validate(descriptor, value);
            return value;
        }
        catch (ConfigValidationException e)
        {
            Object fixedValue = descriptor.copyValue(defaultValue);
            report.repaired(descriptor.path(), value, fixedValue, e.getMessage());
            return fixedValue;
        }
    }
}
