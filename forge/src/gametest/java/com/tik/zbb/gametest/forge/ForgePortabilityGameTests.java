package com.tik.zbb.gametest.forge;

import com.tik.zbb.Constants;
import com.tik.zbb.gametest.PortabilityGameTestLifecycle;
import com.tik.zbb.gametest.PortabilityGameTestReporter;
import com.tik.zbb.gametest.PortabilityGameTestScenarios;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;

import java.util.function.Consumer;

@Mod(PortabilityGameTestScenarios.TEST_MOD_ID)
public final class ForgePortabilityGameTests
{
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, Constants.MOD_ID);

    static
    {
        PortabilityGameTestLifecycle.install(PortabilityGameTestReporter.xmlReporter());
        for (PortabilityGameTestScenarios.Suite suite : PortabilityGameTestScenarios.SUITES)
        {
            for (PortabilityGameTestScenarios.Scenario scenario : suite.scenarios())
            {
                FUNCTIONS.register(scenario.id(), () -> helper -> suite.run(scenario, helper));
            }
        }
    }

    public ForgePortabilityGameTests(FMLJavaModLoadingContext context)
    {
        FUNCTIONS.register(context.getModBusGroup());
    }
}
