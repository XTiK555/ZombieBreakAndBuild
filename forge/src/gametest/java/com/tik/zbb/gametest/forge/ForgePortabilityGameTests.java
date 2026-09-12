package com.tik.zbb.gametest.forge;

import com.tik.zbb.Constants;
import com.tik.zbb.gametest.PortabilityGameTestLifecycle;
import com.tik.zbb.gametest.PortabilityGameTestReporter;
import com.tik.zbb.gametest.PortabilityGameTestScenarios;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.gametest.GameTestHolder;

import java.util.Collection;

@Mod(PortabilityGameTestScenarios.TEST_MOD_ID)
@GameTestHolder(value = Constants.MOD_ID, namespace = Constants.MOD_ID)
public final class ForgePortabilityGameTests
{
    static
    {
        PortabilityGameTestLifecycle.install(PortabilityGameTestReporter.xmlReporter());
    }

    @GameTestGenerator
    public static Collection<TestFunction> tests()
    {
        PortabilityGameTestScenarios.Suite suite = PortabilityGameTestScenarios.selectedSuite();
        return suite.scenarios().stream().map(scenario -> new TestFunction(
                suite.testNamespace() + "." + scenario.id(), suite.testNamespace() + ":" + scenario.id(),
                PortabilityGameTestScenarios.EMPTY_STRUCTURE_ID, scenario.maxTicks(), 0, true,
                helper -> suite.run(scenario, helper))).toList();
    }
}
