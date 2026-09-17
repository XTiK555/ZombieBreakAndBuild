package com.tik.zbb.config.edit;

import com.tik.zbb.config.schema.ConfigFieldDescriptor;
import com.tik.zbb.config.runtime.ConfigAvailabilityReport;
import com.tik.zbb.config.schema.ConfigValidationException;

public interface ConfigSemanticValidator
{
    ConfigSemanticValidator NONE = (descriptor, value) -> {};

    void validate(ConfigFieldDescriptor descriptor, Object value) throws ConfigValidationException;

    default Object resolveValue(
            ConfigFieldDescriptor descriptor,
            Object value,
            Object defaultValue,
            ConfigAvailabilityReport report
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
            report.unavailable(descriptor.path(), value, e.getMessage());
            return fixedValue;
        }
    }
}
