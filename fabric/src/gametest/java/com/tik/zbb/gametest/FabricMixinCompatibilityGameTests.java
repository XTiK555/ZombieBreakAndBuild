package com.tik.zbb.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public final class FabricMixinCompatibilityGameTests
{
    static
    {
        MixinCompatibilityScenarios.installXmlReporter();
    }

    @GameTest(structure = "minecraft:empty", maxTicks = MixinCompatibilityScenarios.BLOCK_AND_ACCESSORS_MAX_TICKS)
    public void blockAndAccessors(GameTestHelper helper)
    {
        MixinCompatibilityScenarios.BLOCK_AND_ACCESSORS.run(helper);
    }

    @GameTest(structure = "minecraft:empty", maxTicks = MixinCompatibilityScenarios.FALLING_BLOCK_MAX_TICKS)
    public void fallingBlock(GameTestHelper helper)
    {
        MixinCompatibilityScenarios.FALLING_BLOCK.run(helper);
    }

    @GameTest(structure = "minecraft:empty", maxTicks = MixinCompatibilityScenarios.NEAREST_TARGET_MAX_TICKS)
    public void nearestTargetThroughWall(GameTestHelper helper)
    {
        MixinCompatibilityScenarios.NEAREST_TARGET.run(helper);
    }

    @GameTest(structure = "minecraft:empty", maxTicks = MixinCompatibilityScenarios.CONTINUE_TARGET_MAX_TICKS)
    public void continueTargetThroughWall(GameTestHelper helper)
    {
        MixinCompatibilityScenarios.CONTINUE_TARGET.run(helper);
    }
}
