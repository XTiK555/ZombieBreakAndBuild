package com.tik.zbb.config.schema;

public interface ConfigValueCodec
{
    Object parseText(ConfigFieldDescriptor descriptor, String rawValue) throws ConfigValidationException;

    Object decodeDocumentValue(ConfigFieldDescriptor descriptor, Object rawValue) throws ConfigValidationException;

    void validateValue(ConfigFieldDescriptor descriptor, Object value) throws ConfigValidationException;
}
