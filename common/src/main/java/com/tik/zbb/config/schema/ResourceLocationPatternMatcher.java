package com.tik.zbb.config.schema;

import net.minecraft.resources.Identifier;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record ResourceLocationPatternMatcher(
        boolean includeAll,
        boolean excludeAll,
        Set<Identifier> includedIds,
        Set<Identifier> excludedIds,
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
        Set<Identifier> includedIds = new HashSet<>();
        Set<Identifier> excludedIds = new HashSet<>();
        Set<String> includedNamespaces = new HashSet<>();
        Set<String> excludedNamespaces = new HashSet<>();
        Set<String> includedPaths = new HashSet<>();
        Set<String> excludedPaths = new HashSet<>();

        for (String rawValue : list)
        {
            String normalized;
            try
            {
                normalized = ResourceLocationPatternParser.normalizeEntry(rawValue);
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
                Identifier id = Identifier.tryParse(body);
                if (id == null) continue;
                if (exclude) excludedIds.add(id);
                else includedIds.add(id);
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

    public boolean matches(Identifier id)
    {
        if (id == null) return false;

        if (excludeAll
                || excludedIds.contains(id)
                || excludedNamespaces.contains(id.getNamespace())
                || excludedPaths.contains(id.getPath()))
        {
            return false;
        }

        return includeAll
                || includedIds.contains(id)
                || includedNamespaces.contains(id.getNamespace())
                || includedPaths.contains(id.getPath());
    }
}
