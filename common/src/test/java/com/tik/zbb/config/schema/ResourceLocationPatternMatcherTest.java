package com.tik.zbb.config.schema;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ResourceLocationPatternMatcherTest
{
    @Test
    void matcherSupportsExactNamespacePathAndGlobalIncludes() throws Exception
    {
        ResourceLocationPatternMatcher matcher = ResourceLocationPatternMatcher.compile(List.of(
                "minecraft:zombie",
                "example:*",
                "*:skeleton",
                "*:*"
        ));

        assertTrue(matches(matcher, "minecraft:zombie"));
        assertTrue(matches(matcher, "example:anything"));
        assertTrue(matches(matcher, "minecraft:skeleton"));
        assertTrue(matches(matcher, "other:creeper"));

        assertTrue(matcher.includeAll());
        assertTrue(identifierSet(matcher, "includedIds").contains(id("minecraft:zombie")));
        assertTrue(matcher.includedNamespaces().contains("example"));
        assertTrue(matcher.includedPaths().contains("skeleton"));
    }

    @Test
    void matcherSupportsEachExcludeBucket() throws Exception
    {
        ResourceLocationPatternMatcher matcher = ResourceLocationPatternMatcher.compile(List.of(
                "*:*",
                "!minecraft:zombie",
                "!example:*",
                "!*:skeleton"
        ));

        assertFalse(matches(matcher, "minecraft:zombie"));
        assertFalse(matches(matcher, "example:creeper"));
        assertFalse(matches(matcher, "minecraft:skeleton"));
        assertTrue(matches(matcher, "minecraft:creeper"));

        assertTrue(identifierSet(matcher, "excludedIds").contains(id("minecraft:zombie")));
        assertTrue(matcher.excludedNamespaces().contains("example"));
        assertTrue(matcher.excludedPaths().contains("skeleton"));
    }

    @Test
    void matcherExcludeWinsRegardlessOfOrder() throws Exception
    {
        ResourceLocationPatternMatcher excludeFirst = ResourceLocationPatternMatcher.compile(List.of(
                "!minecraft:zombie",
                "*:*"
        ));
        ResourceLocationPatternMatcher excludeLast = ResourceLocationPatternMatcher.compile(List.of(
                "*:*",
                "!minecraft:zombie"
        ));

        assertFalse(matches(excludeFirst, "minecraft:zombie"));
        assertFalse(matches(excludeLast, "minecraft:zombie"));
    }

    @Test
    void matcherEmptyListMatchesNothing() throws Exception
    {
        ResourceLocationPatternMatcher matcher = ResourceLocationPatternMatcher.compile(List.of());

        assertFalse(matches(matcher, "minecraft:zombie"));
        assertFalse(matcher.includeAll());
        assertTrue(identifierSet(matcher, "includedIds").isEmpty());
        assertTrue(matcher.includedNamespaces().isEmpty());
        assertTrue(matcher.includedPaths().isEmpty());
    }

    @Test
    void matcherGlobalExcludeWins() throws Exception
    {
        ResourceLocationPatternMatcher matcher = ResourceLocationPatternMatcher.compile(List.of("*:*", "!*:*"));

        assertFalse(matches(matcher, "minecraft:zombie"));
        assertTrue(matcher.includeAll());
        assertTrue(matcher.excludeAll());
    }

    private static boolean matches(ResourceLocationPatternMatcher matcher, String id) throws Exception
    {
        return matcher.matches(id);
    }

    private static Set<?> identifierSet(ResourceLocationPatternMatcher matcher, String method) throws Exception
    {
        return (Set<?>) matcher.getClass().getMethod(method).invoke(matcher);
    }

    private static ResourceLocationId id(String value) throws Exception
    {
        return ResourceLocationId.parse(value);
    }
}
