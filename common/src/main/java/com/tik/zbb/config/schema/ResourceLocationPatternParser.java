package com.tik.zbb.config.schema;

import net.minecraft.resources.Identifier;

public final class ResourceLocationPatternParser
{
    private ResourceLocationPatternParser() {}

    public static String normalizeEntry(String rawValue) throws ConfigValidationException
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
            Identifier id = Identifier.tryParse(body);
            if (id == null) throw new ConfigValidationException("Expected resource location pattern");
            if (id.getNamespace().isBlank() || id.getPath().isBlank())
            {
                throw new ConfigValidationException("Expected resource location pattern");
            }
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

        Identifier sample = Identifier.tryParse(namespace + ":test");
        if (sample == null) throw new ConfigValidationException("Expected resource location pattern");
        return sample.getNamespace();
    }

    private static String normalizeWildcardPath(String path) throws ConfigValidationException
    {
        if ("*".equals(path)) return path;
        if (path.isBlank() || path.contains("*"))
        {
            throw new ConfigValidationException("Expected resource location pattern");
        }

        Identifier sample = Identifier.tryParse("minecraft:" + path);
        if (sample == null) throw new ConfigValidationException("Expected resource location pattern");
        return sample.getPath();
    }
}
