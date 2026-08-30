package com.tik.zbb.gametest;

import com.tik.zbb.Constants;
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

@Mod("zbb_compatibility_test")
public final class NeoForgeMixinCompatibilityGameTests
{
    private static final Identifier EMPTY_STRUCTURE = Identifier.fromNamespaceAndPath("minecraft", "empty");
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, Constants.MOD_ID);
    private static final Map<MixinCompatibilityScenarios.Scenario, DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> REGISTERED = new LinkedHashMap<>();

    static
    {
        MixinCompatibilityScenarios.installXmlReporter();
        for (MixinCompatibilityScenarios.Scenario scenario : MixinCompatibilityScenarios.ALL)
        {
            REGISTERED.put(scenario, FUNCTIONS.register(scenario.id() + "_function", scenario::test));
        }
    }

    public NeoForgeMixinCompatibilityGameTests(IEventBus modBus)
    {
        FUNCTIONS.register(modBus);
        modBus.addListener(NeoForgeMixinCompatibilityGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event)
    {
        Holder<TestEnvironmentDefinition<?>> environment =
                event.registerEnvironment(id("mixin_compatibility_environment"));
        REGISTERED.forEach((scenario, function) -> event.registerTest(id(scenario.id()),
                new FunctionGameTestInstance(function.getKey(),
                        new TestData<>(environment, EMPTY_STRUCTURE, scenario.maxTicks(), 0, true, Rotation.NONE))));
    }

    private static Identifier id(String path)
    {
        return Identifier.fromNamespaceAndPath(Constants.MOD_ID, path);
    }
}
