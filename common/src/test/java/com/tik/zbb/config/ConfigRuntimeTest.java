package com.tik.zbb.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConfigRuntimeTest
{
    @Test
    void createCompilesPatternLists() throws Exception
    {
        ConfigData data = new ConfigData();
        data.blocks.dangerousBlockIdList = List.of("minecraft:*", "!minecraft:dirt");
        data.ai.affectedEntityIdList = List.of("*:*", "!minecraft:zombie");
        data.ai.ignoreBuildEntityIdList = List.of("minecraft:ghast");
        data.ai.ignoreBreakEntityIdList = List.of("*:vex");

        ConfigRuntime runtime = ConfigRuntime.create(data);

        assertTrue(matches(runtime.dangerousBlockIdMatcher(), "minecraft:lava"));
        assertFalse(matches(runtime.dangerousBlockIdMatcher(), "minecraft:dirt"));
        assertTrue(matches(runtime.affectedEntityIdMatcher(), "minecraft:skeleton"));
        assertFalse(matches(runtime.affectedEntityIdMatcher(), "minecraft:zombie"));
        assertTrue(matches(runtime.ignoreBuildEntityIdMatcher(), "minecraft:ghast"));
        assertTrue(matches(runtime.ignoreBreakEntityIdMatcher(), "minecraft:vex"));
    }

    @Test
    void defaultAiModelIsPatternDriven() throws Exception
    {
        ConfigRuntime runtime = ConfigRuntime.create(new ConfigData());

        assertTrue(matches(runtime.affectedEntityIdMatcher(), "minecraft:cow"));
        assertTrue(matches(runtime.affectedEntityIdMatcher(), "example:custom_mob"));
        assertTrue(matches(runtime.ignoreBuildEntityIdMatcher(), "minecraft:ghast"));
        assertTrue(matches(runtime.ignoreBreakEntityIdMatcher(), "minecraft:vex"));
        assertFalse(matches(runtime.ignoreBreakEntityIdMatcher(), "minecraft:zombie"));
    }

    private static boolean matches(Object matcher, String id) throws Exception
    {
        Object identifier = id(id);
        return (boolean) matcher.getClass().getMethod("matches", identifierClass()).invoke(matcher, identifier);
    }

    private static Object id(String value) throws Exception
    {
        Object id = identifierClass().getMethod("tryParse", String.class).invoke(null, value);
        assertNotNull(id);
        return id;
    }

    private static Class<?> identifierClass() throws ClassNotFoundException
    {
        return Class.forName("net.minecraft.resources.Identifier");
    }
}
