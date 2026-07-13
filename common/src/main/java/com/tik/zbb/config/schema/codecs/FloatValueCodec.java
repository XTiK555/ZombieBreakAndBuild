package com.tik.zbb.config.schema.codecs;

import com.tik.zbb.config.schema.ConfigFieldDescriptor;
import com.tik.zbb.config.schema.ConfigValidationException;

public final class FloatValueCodec extends NumberValueCodec
{
    public static final FloatValueCodec INSTANCE = new FloatValueCodec();

    private FloatValueCodec() {}

    @Override
    public Object parseText(ConfigFieldDescriptor descriptor, String rawValue) throws ConfigValidationException
    {
        try
        {
            float value = Float.parseFloat(rawValue);
            validateRange(descriptor, value);
            return value;
        }
        catch (NumberFormatException e)
        {
            throw new ConfigValidationException("Expected decimal number");
        }
    }

    @Override
    public Object decodeDocumentValue(ConfigFieldDescriptor descriptor, Object rawValue) throws ConfigValidationException
    {
        if (!(rawValue instanceof Number n))
        {
            throw new ConfigValidationException("Expected decimal number");
        }

        float value = n.floatValue();
        validateRange(descriptor, value);
        return value;
    }

    @Override
    public Object normalizeValue(ConfigFieldDescriptor descriptor, Object value) throws ConfigValidationException
    {
        if (!(value instanceof Number n))
        {
            throw new ConfigValidationException("Expected decimal number");
        }

        float normalized = n.floatValue();
        validateRange(descriptor, normalized);
        return normalized;
    }
}
