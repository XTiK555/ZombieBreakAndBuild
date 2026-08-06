package com.tik.zbb.utilities;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigUtilitiesTest
{
    @Test
    void configFieldsIncludeInheritedFieldsAndExcludeSyntheticFields()
    {
        Outer outer = new Outer();

        List<String> names = ConfigUtilities.getConfigFields(outer.new ChildConfig().getClass()).stream()
                .map(Field::getName)
                .toList();

        assertEquals(List.of("inherited", "own"), names);
    }

    @Test
    void genericContainersAreSimpleOnlyWhenTheirContentsAreSimple() throws Exception
    {
        assertFalse(ConfigUtilities.isNestedConfigField(ContainerConfig.class.getDeclaredField("strings")));
        assertFalse(ConfigUtilities.isNestedConfigField(ContainerConfig.class.getDeclaredField("numbersByName")));
        assertTrue(ConfigUtilities.isNestedConfigField(ContainerConfig.class.getDeclaredField("sections")));
        assertTrue(ConfigUtilities.isNestedConfigField(ContainerConfig.class.getDeclaredField("sectionsByName")));
        assertFalse(ConfigUtilities.isConfigSectionField(ContainerConfig.class.getDeclaredField("sections")));
        assertTrue(ConfigUtilities.isConfigSectionField(ContainerConfig.class.getDeclaredField("section")));
    }

    @Test
    void deepCopyRecursesIntoConfigObjectsInsideContainers()
    {
        ContainerConfig original = new ContainerConfig();
        original.sections.add(new NestedConfig());
        original.sectionsByName.put("first", new NestedConfig());

        ContainerConfig copy = (ContainerConfig) ConfigUtilities.deepCopyConfigValue(original);
        copy.sections.getFirst().value = 2;
        copy.sectionsByName.get("first").value = 3;

        assertEquals(1, original.sections.getFirst().value);
        assertEquals(1, original.sectionsByName.get("first").value);
    }

    @Test
    void deepCopyPreservesAliasesAndCyclesWithoutSharingMutableObjects()
    {
        RecursiveConfig original = new RecursiveConfig();
        original.alias = original.child;
        original.self = original;

        RecursiveConfig copy = (RecursiveConfig) ConfigUtilities.deepCopyConfigValue(original);

        assertNotSame(original, copy);
        assertNotSame(original.child, copy.child);
        assertSame(copy.child, copy.alias);
        assertSame(copy, copy.self);
    }

    private static class ParentConfig
    {
        int inherited;
        transient int transientField;
        static int staticField;
    }

    private class Outer
    {
        private class ChildConfig extends ParentConfig
        {
            int own;
        }
    }

    private static class ContainerConfig
    {
        List<String> strings = new java.util.ArrayList<>();
        Map<String, Integer> numbersByName = new java.util.LinkedHashMap<>();
        List<NestedConfig> sections = new java.util.ArrayList<>();
        Map<String, NestedConfig> sectionsByName = new java.util.LinkedHashMap<>();
        NestedConfig section = new NestedConfig();
    }

    private static class NestedConfig
    {
        int value = 1;
    }

    private static class RecursiveConfig
    {
        NestedConfig child = new NestedConfig();
        NestedConfig alias;
        RecursiveConfig self;
    }
}
