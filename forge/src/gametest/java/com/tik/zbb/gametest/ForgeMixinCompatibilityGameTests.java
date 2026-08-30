package com.tik.zbb.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@Mod("zbb_compatibility_test")
@GameTestHolder("minecraft")
@PrefixGameTestTemplate(false)
public final class ForgeMixinCompatibilityGameTests
{
    static
    {
        MixinCompatibilityScenarios.installXmlReporter();
    }

    @GameTest(template = "empty", timeoutTicks = MixinCompatibilityScenarios.BLOCK_AND_ACCESSORS_MAX_TICKS)
    public static void block_and_accessors(GameTestHelper helper)
    {
        MixinCompatibilityScenarios.BLOCK_AND_ACCESSORS.run(helper);
    }

    @GameTest(template = "empty", timeoutTicks = MixinCompatibilityScenarios.FALLING_BLOCK_MAX_TICKS)
    public static void falling_block(GameTestHelper helper)
    {
        MixinCompatibilityScenarios.FALLING_BLOCK.run(helper);
    }

    @GameTest(template = "empty", timeoutTicks = MixinCompatibilityScenarios.NEAREST_TARGET_MAX_TICKS)
    public static void nearest_target_through_wall(GameTestHelper helper)
    {
        MixinCompatibilityScenarios.NEAREST_TARGET.run(helper);
    }

    @GameTest(template = "empty", timeoutTicks = MixinCompatibilityScenarios.CONTINUE_TARGET_MAX_TICKS)
    public static void continue_target_through_wall(GameTestHelper helper)
    {
        MixinCompatibilityScenarios.CONTINUE_TARGET.run(helper);
    }
}
