package com.tik.zbb.config.schema.codecs;

import com.tik.zbb.config.schema.ConfigFieldDescriptor;
import com.tik.zbb.config.schema.ResourceLocationId;
import com.tik.zbb.config.schema.ConfigValidationException;
import com.tik.zbb.config.schema.ConfigValueCodec;

public final class ResourceLocationValueCodec implements ConfigValueCodec
{
    public static final ResourceLocationValueCodec INSTANCE = new ResourceLocationValueCodec();

    private ResourceLocationValueCodec() {}

    @Override
    public Object parseText(ConfigFieldDescriptor descriptor, String rawValue) throws ConfigValidationException
    {
        return normalize(rawValue);
    }

    @Override
    public Object decodeDocumentValue(ConfigFieldDescriptor descriptor, Object rawValue) throws ConfigValidationException
    {
        if (rawValue instanceof String s) return normalize(s);
        throw new ConfigValidationException("Expected resource location");
    }

    @Override
    public Object normalizeValue(ConfigFieldDescriptor descriptor, Object value) throws ConfigValidationException
    {
        if (value instanceof String s)
        {
            return normalize(s);
        }
        throw new ConfigValidationException("Expected resource location");
    }

    static String normalize(String rawValue) throws ConfigValidationException
    {
        return ResourceLocationId.normalize(rawValue);
    }
}
