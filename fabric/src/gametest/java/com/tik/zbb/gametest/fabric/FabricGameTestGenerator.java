package com.tik.zbb.gametest.fabric;

import com.tik.zbb.gametest.PortabilityGameTestScenarios;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

public final class FabricGameTestGenerator
{
    private FabricGameTestGenerator() {}

    public static void main(String[] args) throws Exception
    {
        if (args.length != 2) throw new IllegalArgumentException("Expected: <output-java-path> <output-resources>");
        generate(Path.of(args[0]), Path.of(args[1]));
    }

    private static void generate(Path output, Path resources) throws Exception
    {
        Files.createDirectories(output.getParent());
        StringBuilder source = new StringBuilder("""
                package com.tik.zbb.gametest;

import net.fabricmc.loader.api.LanguageAdapter;
import net.fabricmc.loader.api.LanguageAdapterException;
import net.fabricmc.loader.api.ModContainer;
                import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

                public final class FabricPortabilityGameTests implements LanguageAdapter
                {
                    @Override
                    public <T> T create(ModContainer mod, String value, Class<T> type) throws LanguageAdapterException
                    {
                        PortabilityGameTestLifecycle.install(PortabilityGameTestReporter.xmlReporter());
                        return type.cast(SUITES.get(PortabilityGameTestScenarios.selectedSuite().id()));
                    }

                """);
        source.append("    private static final java.util.Map<String, Object> SUITES = java.util.Map.of(\n");
        for (int suiteIndex = 0; suiteIndex < PortabilityGameTestScenarios.SUITES.size(); suiteIndex++)
        {
            PortabilityGameTestScenarios.Suite suite = PortabilityGameTestScenarios.SUITES.get(suiteIndex);
            source.append("            \"").append(suite.id()).append("\", new ")
                    .append(toUpperCamel(suite.normalizedId())).append("()")
                    .append(suiteIndex + 1 == PortabilityGameTestScenarios.SUITES.size() ? "\n" : ",\n");
        }
        source.append("    );\n\n");
        for (PortabilityGameTestScenarios.Suite suite : PortabilityGameTestScenarios.SUITES)
        {
            source.append("    public static final class ").append(toUpperCamel(suite.normalizedId())).append("\n    {\n");
            for (PortabilityGameTestScenarios.Scenario scenario : suite.scenarios())
            {
                source.append("        @GameTest(template = \"").append(PortabilityGameTestScenarios.EMPTY_STRUCTURE_ID)
                        .append("\", batch = \"").append(suite.normalizedId()).append(".").append(scenario.id())
                        .append("\", timeoutTicks = ").append(scenario.maxTicks()).append(")\n")
                        .append("        public void ").append(scenario.id()).append("(GameTestHelper helper)\n")
                        .append("        {\n            PortabilityGameTestScenarios.Suite suite = PortabilityGameTestScenarios.suite(\"").append(suite.id())
                        .append("\");\n            suite.run(suite.scenario(\"").append(scenario.id())
                        .append("\"), helper);\n        }\n\n");
            }
            source.append("    }\n\n");
        }
        source.append("}\n");
        Files.writeString(output, source);

        clearDirectory(resources);
        for (PortabilityGameTestScenarios.Suite suite : PortabilityGameTestScenarios.SUITES)
        {
            Set<String> generatedEnvironments = new HashSet<>();
            for (PortabilityGameTestScenarios.Scenario scenario : suite.scenarios())
            {
                String environmentName = suite.environmentName(scenario);
                if (!generatedEnvironments.add(environmentName)) continue;
                Path environment = resources.resolve("data").resolve(suite.testNamespace())
                        .resolve("test_environment").resolve(environmentName + ".json");
                Files.createDirectories(environment.getParent());
                Files.writeString(environment, "{\"type\":\"minecraft:all_of\",\"definitions\":[]}\n");
            }
        }
    }

    private static void clearDirectory(Path output) throws Exception
    {
        if (!Files.isDirectory(output)) return;
        try (var paths = Files.walk(output))
        {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
        }
    }

    private static String toLowerCamel(String id)
    {
        StringBuilder result = new StringBuilder();
        boolean capitalize = false;
        for (char character : id.toCharArray())
        {
            if (character == '_') capitalize = true;
            else
            {
                result.append(capitalize ? Character.toUpperCase(character) : character);
                capitalize = false;
            }
        }
        return result.toString();
    }

    private static String toUpperCamel(String id)
    {
        String lowerCamel = toLowerCamel(id);
        return Character.toUpperCase(lowerCamel.charAt(0)) + lowerCamel.substring(1);
    }
}
