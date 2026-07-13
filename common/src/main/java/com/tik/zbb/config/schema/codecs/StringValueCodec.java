package com.tik.zbb.config.schema.codecs;

import com.tik.zbb.config.schema.ConfigFieldDescriptor;
import com.tik.zbb.config.schema.ConfigValidationException;
import com.tik.zbb.config.schema.ConfigValueCodec;

public final class StringValueCodec implements ConfigValueCodec
{
    public static final StringValueCodec INSTANCE = new StringValueCodec();

    private StringValueCodec() {}

    @Override
    public Object parseText(ConfigFieldDescriptor descriptor, String rawValue)
    {
        return rawValue;
    }

    @Override
    public Object decodeDocumentValue(ConfigFieldDescriptor descriptor, Object rawValue) throws ConfigValidationException
    {
        if (rawValue instanceof String s) return s;
        throw new ConfigValidationException("Expected string");
    }

    @Override
    public Object normalizeValue(ConfigFieldDescriptor descriptor, Object value) throws ConfigValidationException
    {
        if (value instanceof String) return value;
        throw new ConfigValidationException("Expected string");
    }
}
