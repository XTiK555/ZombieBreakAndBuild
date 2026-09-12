package com.tik.zbb.gametest.neoforge;

import com.tik.zbb.Constants;
import com.tik.zbb.gametest.PortabilityGameTestLifecycle;
import com.tik.zbb.gametest.PortabilityGameTestReporter;
import com.tik.zbb.gametest.PortabilityGameTestScenarios;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

@Mod(PortabilityGameTestScenarios.TEST_MOD_ID)
public final class NeoForgePortabilityGameTests
{
    private static final Identifier TEST_STRUCTURE = Identifier.parse(PortabilityGameTestScenarios.EMPTY_STRUCTURE_ID);
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, Constants.MOD_ID);
    private static final Map<PortabilityGameTestScenarios.Scenario, DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> REGISTERED = new LinkedHashMap<>();

    static
    {
        PortabilityGameTestLifecycle.install(PortabilityGameTestReporter.xmlReporter());
        PortabilityGameTestScenarios.Suite suite = PortabilityGameTestScenarios.selectedSuite();
        for (PortabilityGameTestScenarios.Scenario scenario : suite.scenarios())
        {
            REGISTERED.put(scenario, FUNCTIONS.register(scenario.id() + "_function",
                    () -> helper -> suite.run(scenario, helper)));
        }
    }

    public NeoForgePortabilityGameTests(IEventBus modBus)
    {
        FUNCTIONS.register(modBus);
        modBus.addListener(NeoForgePortabilityGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event)
    {
        PortabilityGameTestScenarios.Suite suite = PortabilityGameTestScenarios.selectedSuite();
        String namespace = suite.testNamespace();
        Map<String, Holder<TestEnvironmentDefinition>> environments = new LinkedHashMap<>();
        REGISTERED.forEach((scenario, function) ->
        {
            String environmentId = suite.environmentId(scenario);
            Holder<TestEnvironmentDefinition> environment = environments.computeIfAbsent(environmentId,
                    id -> event.registerEnvironment(Identifier.parse(id)));
            event.registerTest(Identifier.fromNamespaceAndPath(namespace, scenario.id()),
                    new FunctionGameTestInstance(function.getKey(),
                            new TestData<>(environment, TEST_STRUCTURE, scenario.maxTicks(), 0, true, Rotation.NONE)));
        });
    }

}
