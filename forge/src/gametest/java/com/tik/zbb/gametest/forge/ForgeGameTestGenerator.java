package com.tik.zbb.gametest.forge;

import com.tik.zbb.Constants;
import com.tik.zbb.gametest.PortabilityGameTestScenarios;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public final class ForgeGameTestGenerator
{
    private static final String TEST_INSTANCE = """
            {
              "type": "minecraft:function",
              "function": "%s",
              "environment": "%s",
              "structure": "%s",
              "max_ticks": %d
            }
            """;

    private ForgeGameTestGenerator() {}

    public static void main(String[] args) throws Exception
    {
        if (args.length != 1) throw new IllegalArgumentException("Expected: <output-directory>");
        generate(Path.of(args[0]));
    }

    private static void generate(Path output) throws Exception
    {
        for (PortabilityGameTestScenarios.Suite suite : PortabilityGameTestScenarios.SUITES)
        {
            Path suiteOutput = output.resolve("data").resolve(suite.testNamespace()).resolve("test_instance");
            Path environmentOutput = output.resolve("data").resolve(suite.testNamespace()).resolve("test_environment");
            Files.createDirectories(suiteOutput);
            Files.createDirectories(environmentOutput);
            clearJsonFiles(suiteOutput);
            clearJsonFiles(environmentOutput);
            Set<String> generatedEnvironments = new HashSet<>();
            for (PortabilityGameTestScenarios.Scenario scenario : suite.scenarios())
            {
                String environmentId = suite.environmentId(scenario);
                Files.writeString(suiteOutput.resolve(scenario.id() + ".json"), TEST_INSTANCE.formatted(
                        Constants.MOD_ID + ":" + scenario.id(), environmentId,
                        PortabilityGameTestScenarios.EMPTY_STRUCTURE_ID,
                        scenario.maxTicks()));
                String environmentName = suite.environmentName(scenario);
                if (generatedEnvironments.add(environmentName))
                    Files.writeString(environmentOutput.resolve(environmentName + ".json"),
                            "{\"type\":\"minecraft:all_of\",\"definitions\":[]}\n");
            }
        }
    }

    private static void clearJsonFiles(Path output) throws Exception
    {
        if (!Files.isDirectory(output)) return;
        try (var files = Files.list(output))
        {
            for (Path file : files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json")).toList())
                Files.delete(file);
        }
    }
}
