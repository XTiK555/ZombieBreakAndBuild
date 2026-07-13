package com.tik.zbb.config.schema.codecs;

import com.tik.zbb.config.schema.ConfigFieldDescriptor;
import com.tik.zbb.config.schema.ConfigValidationException;

public final class DoubleValueCodec extends NumberValueCodec
{
    public static final DoubleValueCodec INSTANCE = new DoubleValueCodec();

    private DoubleValueCodec() {}

    @Override
    public Object parseText(ConfigFieldDescriptor descriptor, String rawValue) throws ConfigValidationException
    {
        try
        {
            double value = Double.parseDouble(rawValue);
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

        double value = n.doubleValue();
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
        double normalized = n.doubleValue();
        validateRange(descriptor, normalized);
        return normalized;
    }
}
