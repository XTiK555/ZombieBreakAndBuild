package com.tik.zbb.config.schema;

import com.tik.zbb.config.schema.codecs.ResourceLocationPatternListCodec;
import net.minecraft.resources.Identifier;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record ResourceLocationPatternMatcher(
        boolean includeAll,
        boolean excludeAll,
        Set<ResourceLocationId> includedIds,
        Set<ResourceLocationId> excludedIds,
        Set<String> includedNamespaces,
        Set<String> excludedNamespaces,
        Set<String> includedPaths,
        Set<String> excludedPaths
)
{
    public static ResourceLocationPatternMatcher compile(List<String> list)
    {
        boolean includeAll = false;
        boolean excludeAll = false;
        Set<ResourceLocationId> includedIds = new HashSet<>();
        Set<ResourceLocationId> excludedIds = new HashSet<>();
        Set<String> includedNamespaces = new HashSet<>();
        Set<String> excludedNamespaces = new HashSet<>();
        Set<String> includedPaths = new HashSet<>();
        Set<String> excludedPaths = new HashSet<>();

        for (String rawValue : list)
        {
            String normalized;
            try
            {
                normalized = ResourceLocationPatternListCodec.normalizePattern(rawValue);
            }
            catch (ConfigValidationException ignored)
            {
                continue;
            }

            boolean exclude = normalized.startsWith("!");
            String body = exclude ? normalized.substring(1) : normalized;
            String[] parts = body.split(":", 2);
            String namespace = parts[0];
            String path = parts[1];

            if ("*".equals(namespace) && "*".equals(path))
            {
                if (exclude) excludeAll = true;
                else includeAll = true;
            }
            else if ("*".equals(path))
            {
                if (exclude) excludedNamespaces.add(namespace);
                else includedNamespaces.add(namespace);
            }
            else if ("*".equals(namespace))
            {
                if (exclude) excludedPaths.add(path);
                else includedPaths.add(path);
            }
            else
            {
                try
                {
                    ResourceLocationId id = ResourceLocationId.parse(body);
                    if (exclude) excludedIds.add(id);
                    else includedIds.add(id);
                }
                catch (ConfigValidationException ignored)
                {
                }
            }
        }

        return new ResourceLocationPatternMatcher(
                includeAll,
                excludeAll,
                Set.copyOf(includedIds),
                Set.copyOf(excludedIds),
                Set.copyOf(includedNamespaces),
                Set.copyOf(excludedNamespaces),
                Set.copyOf(includedPaths),
                Set.copyOf(excludedPaths)
        );
    }

    public boolean matches(String rawId)
    {
        try
        {
            return matches(ResourceLocationId.parse(rawId));
        }
        catch (ConfigValidationException e)
        {
            return false;
        }
    }

    public boolean matches(Identifier id)
    {
        if (id == null)
        {
            return false;
        }

        return matches(id.getNamespace(), id.getPath());
    }

    public boolean matches(ResourceLocationId id)
    {
        if (id == null)
        {
            return false;
        }

        return matches(id.namespace(), id.path());
    }

    private boolean matches(String namespace, String path)
    {
        if (excludeAll
                || excludedIds.contains(new ResourceLocationId(namespace, path))
                || excludedNamespaces.contains(namespace)
                || excludedPaths.contains(path))
        {
            return false;
        }

        return includeAll
                || includedIds.contains(new ResourceLocationId(namespace, path))
                || includedNamespaces.contains(namespace)
                || includedPaths.contains(path);
    }
}
