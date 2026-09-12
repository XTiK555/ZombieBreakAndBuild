package com.tik.zbb.gametest.neoforge;

import com.tik.zbb.Constants;
import com.tik.zbb.gametest.PortabilityGameTestLifecycle;
import com.tik.zbb.gametest.PortabilityGameTestReporter;
import com.tik.zbb.gametest.PortabilityGameTestScenarios;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.TestFunction;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.util.Collection;

@Mod(PortabilityGameTestScenarios.TEST_MOD_ID)
@GameTestHolder(Constants.MOD_ID)
public final class NeoForgePortabilityGameTests
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
