package com.tik.zbb.config.schema;

public record ResourceLocationId(String namespace, String path)
{
    private static final String DEFAULT_NAMESPACE = "minecraft";

    public ResourceLocationId
    {
        if (!isValidNamespace(namespace) || !isValidPath(path))
        {
            throw new IllegalArgumentException("Invalid resource location");
        }
    }

    public static ResourceLocationId parse(String rawValue) throws ConfigValidationException
    {
        String value = rawValue.trim();
        int separator = value.indexOf(':');

        String namespace = separator >= 0 ? value.substring(0, separator) : DEFAULT_NAMESPACE;
        String path = separator >= 0 ? value.substring(separator + 1) : value;

        if (!isValidNamespace(namespace) || !isValidPath(path))
        {
            throw new ConfigValidationException("Expected resource location");
        }

        return new ResourceLocationId(namespace, path);
    }

    public static String normalize(String rawValue) throws ConfigValidationException
    {
        return parse(rawValue).toString();
    }

    public static String normalizeNamespace(String namespace) throws ConfigValidationException
    {
        if (!isValidNamespace(namespace))
        {
            throw new ConfigValidationException("Expected resource location pattern");
        }
        return namespace;
    }

    public static String normalizePath(String path) throws ConfigValidationException
    {
        if (!isValidPath(path))
        {
            throw new ConfigValidationException("Expected resource location pattern");
        }
        return path;
    }

    private static boolean isValidNamespace(String value)
    {
        if (value.isBlank()) return false;
        for (int i = 0; i < value.length(); i++)
        {
            char c = value.charAt(i);
            if (!((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '.'))
            {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidPath(String value)
    {
        if (value.isBlank()) return false;
        for (int i = 0; i < value.length(); i++)
        {
            char c = value.charAt(i);
            if (!((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '.' || c == '/'))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString()
    {
        return namespace + ":" + path;
    }
}
