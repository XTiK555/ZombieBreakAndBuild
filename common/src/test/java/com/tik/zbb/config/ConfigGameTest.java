package com.tik.zbb.config;

import com.tik.zbb.config.schema.ResourceLocationPatternMatcher;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigGameTest
{
    @BeforeAll
    static void bootstrapMinecraft()
    {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

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

    @Test
    void tacticsAreEnabledByDefaultAndCanBeDisabledIndividually()
    {
        ConfigDocument defaults = new ConfigDocument();
        ConfigGame.Tactics defaultTactics = ConfigGame.create(defaults).ai().tactics();

        assertTrue(defaultTactics.adjustHeightToTarget());
        assertTrue(defaultTactics.bridgeToTarget());
        assertTrue(defaultTactics.clearObstaclesToTarget());
        assertTrue(defaultTactics.mitigateDangerousBlocks());

        defaults.ai.tactics.adjustHeightToTarget = false;
        defaults.ai.tactics.bridgeToTarget = false;
        defaults.ai.tactics.clearObstaclesToTarget = false;
        defaults.ai.tactics.mitigateDangerousBlocks = false;
        ConfigGame.Tactics disabledTactics = ConfigGame.create(defaults).ai().tactics();

        assertFalse(disabledTactics.adjustHeightToTarget());
        assertFalse(disabledTactics.bridgeToTarget());
        assertFalse(disabledTactics.clearObstaclesToTarget());
        assertFalse(disabledTactics.mitigateDangerousBlocks());
    }

    @Test
    void zeroMaximumHardnessIsUnlimited()
    {
        ConfigGame.BlockDamage blockDamage = ConfigGame.create(new ConfigDocument()).balance().blockDamage();

        assertFalse(exceedsMaximumBreakableHardness(Float.MAX_VALUE, blockDamage));
    }

    @Test
    void configuredMaximumHardnessRejectsOnlyHarderBlocks()
    {
        ConfigDocument data = new ConfigDocument();
        data.balance.blockDamage.maximumBreakableBlockHardness = 5.0f;
        ConfigGame.BlockDamage blockDamage = ConfigGame.create(data).balance().blockDamage();

        assertFalse(exceedsMaximumBreakableHardness(5.0f, blockDamage));
        assertTrue(exceedsMaximumBreakableHardness(5.01f, blockDamage));
    }

    @Test
    void positionParticleCanBeDisabled()
    {
        ConfigDocument data = new ConfigDocument();
        assertTrue(ConfigGame.create(data).visualEffects().brokenReappearMarkerParticle());

        data.visualEffects.brokenReappearMarkerParticle = false;
        assertFalse(ConfigGame.create(data).visualEffects().brokenReappearMarkerParticle());
    }

    private boolean exceedsMaximumBreakableHardness(float hardness, ConfigGame.BlockDamage blockDamage)
    {
        return blockDamage.maximumBreakableBlockHardness() > 0.0f && hardness > blockDamage.maximumBreakableBlockHardness();
    }

    private static boolean matches(ResourceLocationPatternMatcher matcher, String id)
    {
        return matcher.matches(id);
    }
}
