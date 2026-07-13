package com.tik.zbb.config.schema.codecs;

import com.tik.zbb.config.schema.ConfigValidationException;

public final class ResourceLocationIntPairMapCodec extends ResourceLocationMapCodec
{
    public static final ResourceLocationIntPairMapCodec INSTANCE = new ResourceLocationIntPairMapCodec();

    private ResourceLocationIntPairMapCodec() {}

    @Override
    protected Object normalizeValue(Object rawValue) throws ConfigValidationException
    {
        int value;
        if (rawValue instanceof Number n)
        {
            value = NumberValueCodec.exactInt(n, valueError());
        }
        else if (rawValue instanceof String s)
        {
            try
            {
                value = Integer.parseInt(s.trim());
            }
            catch (NumberFormatException e)
            {
                throw new ConfigValidationException(valueError());
            }
        }
        else
        {
            throw new ConfigValidationException(valueError());
        }

        if (value < 0) throw new ConfigValidationException("Expected non-negative integer value");
        return value;
    }

    @Override
    protected String valueError()
    {
        return "Expected integer value";
    }
}
