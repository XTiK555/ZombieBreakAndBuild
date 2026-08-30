package com.tik.zbb.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public final class FabricMixinCompatibilityGameTests implements FabricGameTest
{
    static
    {
        MixinCompatibilityScenarios.installXmlReporter();
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = MixinCompatibilityScenarios.BLOCK_AND_ACCESSORS_MAX_TICKS)
    public void block_and_accessors(GameTestHelper helper)
    {
        MixinCompatibilityScenarios.BLOCK_AND_ACCESSORS.run(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = MixinCompatibilityScenarios.FALLING_BLOCK_MAX_TICKS)
    public void falling_block(GameTestHelper helper)
    {
        MixinCompatibilityScenarios.FALLING_BLOCK.run(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = MixinCompatibilityScenarios.NEAREST_TARGET_MAX_TICKS)
    public void nearest_target_through_wall(GameTestHelper helper)
    {
        MixinCompatibilityScenarios.NEAREST_TARGET.run(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = MixinCompatibilityScenarios.CONTINUE_TARGET_MAX_TICKS)
    public void continue_target_through_wall(GameTestHelper helper)
    {
        MixinCompatibilityScenarios.CONTINUE_TARGET.run(helper);
    }
}
