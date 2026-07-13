package com.tik.zbb.config.schema;

public interface ConfigValueCodec
{
    Object parseText(ConfigFieldDescriptor descriptor, String rawValue) throws ConfigValidationException;

    Object decodeDocumentValue(ConfigFieldDescriptor descriptor, Object rawValue) throws ConfigValidationException;

    Object normalizeValue(ConfigFieldDescriptor descriptor, Object value) throws ConfigValidationException;

    default Object repairDocumentValue(ConfigFieldDescriptor descriptor, Object rawValue, Object defaultValue, ConfigRepairReport report)
    {
        try
        {
            return decodeDocumentValue(descriptor, rawValue);
        }
        catch (ConfigValidationException e)
        {
            Object fixedValue = descriptor.copyValue(defaultValue);
            report.repaired(descriptor.path(), rawValue, fixedValue, e.getMessage());
            return fixedValue;
        }
    }

    default boolean supportsCollectionEdits()
    {
        return false;
    }

    default Object emptyValue(ConfigFieldDescriptor descriptor)
    {
        throw new UnsupportedOperationException("Not a collection config value");
    }

    default Object parseEntry(ConfigFieldDescriptor descriptor, String rawValue) throws ConfigValidationException
    {
        throw new ConfigValidationException("Expected scalar value");
    }

    default Object addEntry(ConfigFieldDescriptor descriptor, Object value, Object entry) throws ConfigValidationException
    {
        throw new ConfigValidationException("Expected scalar value");
    }

    default Object removeEntry(ConfigFieldDescriptor descriptor, Object value, Object entry) throws ConfigValidationException
    {
        throw new ConfigValidationException("Expected scalar value");
    }
}
