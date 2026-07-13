package com.tik.zbb.config.schema.codecs;

import com.tik.zbb.config.schema.ConfigFieldDescriptor;
import com.tik.zbb.config.schema.ConfigValidationException;
import com.tik.zbb.config.schema.ConfigValueCodec;

public final class BooleanValueCodec implements ConfigValueCodec
{
    public static final BooleanValueCodec INSTANCE = new BooleanValueCodec();

    private BooleanValueCodec() {}

    @Override
    public Object parseText(ConfigFieldDescriptor descriptor, String rawValue) throws ConfigValidationException
    {
        if ("true".equalsIgnoreCase(rawValue)) return true;
        if ("false".equalsIgnoreCase(rawValue)) return false;
        throw new ConfigValidationException("Expected true or false");
    }

    @Override
    public Object decodeDocumentValue(ConfigFieldDescriptor descriptor, Object rawValue) throws ConfigValidationException
    {
        if (rawValue instanceof Boolean) return rawValue;
        throw new ConfigValidationException("Expected boolean");
    }

    @Override
    public Object normalizeValue(ConfigFieldDescriptor descriptor, Object value) throws ConfigValidationException
    {
        if (value instanceof Boolean) return value;
        throw new ConfigValidationException("Expected boolean");
    }
}
