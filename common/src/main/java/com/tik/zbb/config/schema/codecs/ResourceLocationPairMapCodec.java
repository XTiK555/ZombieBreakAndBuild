package com.tik.zbb.config.schema.codecs;

import com.tik.zbb.config.schema.ConfigValidationException;

public final class ResourceLocationPairMapCodec extends ResourceLocationMapCodec
{
    public static final ResourceLocationPairMapCodec INSTANCE = new ResourceLocationPairMapCodec();

    private ResourceLocationPairMapCodec() {}

    @Override
    protected Object normalizeValue(Object rawValue) throws ConfigValidationException
    {
        if (rawValue instanceof String s)
        {
            return normalizeIdentifier(s);
        }
        throw new ConfigValidationException(valueError());
    }

    @Override
    protected String valueError()
    {
        return "Expected resource location value";
    }
}
