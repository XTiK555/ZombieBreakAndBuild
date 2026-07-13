package com.tik.zbb.config.schema.codecs;

import com.tik.zbb.config.schema.ConfigValidationException;
import com.tik.zbb.config.schema.ResourceLocationId;

public final class ResourceLocationPatternListCodec extends StringListValueCodec
{
    public static final ResourceLocationPatternListCodec INSTANCE = new ResourceLocationPatternListCodec();

    private ResourceLocationPatternListCodec() {}

    @Override
    protected String normalizeEntry(String rawValue) throws ConfigValidationException
    {
        return normalizePattern(rawValue);
    }

    public static String normalizePattern(String rawValue) throws ConfigValidationException
    {
        String value = rawValue.trim();
        if (value.isBlank()) throw new ConfigValidationException("List entry cannot be empty");

        boolean exclude = value.startsWith("!");
        String body = exclude ? value.substring(1) : value;
        if (body.isBlank() || body.contains("!"))
        {
            throw new ConfigValidationException("Expected resource location pattern");
        }

        if (!body.contains("*"))
        {
            ResourceLocationId id = parsePatternId(body);
            return exclude ? "!" + id : id.toString();
        }

        String[] parts = body.split(":", -1);
        if (parts.length != 2)
        {
            throw new ConfigValidationException("Expected resource location pattern");
        }

        String namespace = normalizeWildcardNamespace(parts[0]);
        String path = normalizeWildcardPath(parts[1]);
        return (exclude ? "!" : "") + namespace + ":" + path;
    }

    private static String normalizeWildcardNamespace(String namespace) throws ConfigValidationException
    {
        if ("*".equals(namespace)) return namespace;
        if (namespace.isBlank() || namespace.contains("*"))
        {
            throw new ConfigValidationException("Expected resource location pattern");
        }

        return ResourceLocationId.normalizeNamespace(namespace);
    }

    private static String normalizeWildcardPath(String path) throws ConfigValidationException
    {
        if ("*".equals(path)) return path;
        if (path.isBlank() || path.contains("*"))
        {
            throw new ConfigValidationException("Expected resource location pattern");
        }

        return ResourceLocationId.normalizePath(path);
    }

    private static ResourceLocationId parsePatternId(String rawValue) throws ConfigValidationException
    {
        try
        {
            return ResourceLocationId.parse(rawValue);
        }
        catch (ConfigValidationException e)
        {
            throw new ConfigValidationException("Expected resource location pattern");
        }
    }
}
