package com.tik.zbb.config.schema.codecs;

import com.tik.zbb.config.schema.ConfigFieldDescriptor;
import com.tik.zbb.config.schema.ConfigValidationException;

public final class IntValueCodec extends NumberValueCodec
{
    public static final IntValueCodec INSTANCE = new IntValueCodec();

    private IntValueCodec() {}

    @Override
    public Object parseText(ConfigFieldDescriptor descriptor, String rawValue) throws ConfigValidationException
    {
        try
        {
            int value = Integer.parseInt(rawValue);
            validateRange(descriptor, value);
            return value;
        }
        catch (NumberFormatException e)
        {
            throw new ConfigValidationException("Expected integer");
        }
    }

    @Override
    public Object decodeDocumentValue(ConfigFieldDescriptor descriptor, Object rawValue) throws ConfigValidationException
    {
        if (!(rawValue instanceof Number n))
        {
            throw new ConfigValidationException("Expected integer");
        }

        int value = exactInt(n, "Expected integer");
        validateRange(descriptor, value);
        return value;
    }

    @Override
    public Object normalizeValue(ConfigFieldDescriptor descriptor, Object value) throws ConfigValidationException
    {
        if (!(value instanceof Number n))
        {
            throw new ConfigValidationException("Expected integer");
        }
        int normalized = exactInt(n, "Expected integer");
        validateRange(descriptor, normalized);
        return normalized;
    }
}
