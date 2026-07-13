package com.tik.zbb.config;

import org.junit.jupiter.api.Test;
import com.tik.zbb.config.schema.ResourceLocationPatternMatcher;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConfigGameTest
{
    @Test
    void createCompilesPatternLists() throws Exception
    {
        ConfigDocument data = new ConfigDocument();
        data.blocks.dangerousBlockIdList = List.of("minecraft:*", "!minecraft:dirt");
        data.ai.affectedEntityIdList = List.of("*:*", "!minecraft:zombie");
        data.ai.ignoreBuildEntityIdList = List.of("minecraft:ghast");
        data.ai.ignoreBreakEntityIdList = List.of("*:vex");

        ConfigGame game = ConfigGame.create(data);

        assertTrue(matches(game.blocks().dangerousBlockIdMatcher(), "minecraft:lava"));
        assertFalse(matches(game.blocks().dangerousBlockIdMatcher(), "minecraft:dirt"));
        assertTrue(matches(game.ai().affectedEntityIdMatcher(), "minecraft:skeleton"));
        assertFalse(matches(game.ai().affectedEntityIdMatcher(), "minecraft:zombie"));
        assertTrue(matches(game.ai().ignoreBuildEntityIdMatcher(), "minecraft:ghast"));
        assertTrue(matches(game.ai().ignoreBreakEntityIdMatcher(), "minecraft:vex"));
    }

    @Test
    void defaultAiModelIsPatternDriven() throws Exception
    {
        ConfigGame game = ConfigGame.create(new ConfigDocument());

        assertTrue(matches(game.ai().affectedEntityIdMatcher(), "minecraft:cow"));
        assertTrue(matches(game.ai().affectedEntityIdMatcher(), "example:custom_mob"));
        assertTrue(matches(game.ai().ignoreBuildEntityIdMatcher(), "minecraft:ghast"));
        assertFalse(matches(game.ai().ignoreBreakEntityIdMatcher(), "minecraft:vex"));
        assertFalse(matches(game.ai().ignoreBreakEntityIdMatcher(), "minecraft:zombie"));
    }

    private static boolean matches(ResourceLocationPatternMatcher matcher, String id)
    {
        return matcher.matches(id);
    }
}
