package com.tik.zbb.gametest;

import com.tik.zbb.Constants;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;

import java.util.function.Consumer;

@Mod("zbb_compatibility_test")
public final class ForgeMixinCompatibilityGameTests
{
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, Constants.MOD_ID);

    static
    {
        MixinCompatibilityScenarios.installXmlReporter();
        for (MixinCompatibilityScenarios.Scenario scenario : MixinCompatibilityScenarios.ALL)
        {
            FUNCTIONS.register(scenario.id(), scenario::test);
        }
    }

    public ForgeMixinCompatibilityGameTests(FMLJavaModLoadingContext context)
    {
        FUNCTIONS.register(context.getModBusGroup());
    }
}
