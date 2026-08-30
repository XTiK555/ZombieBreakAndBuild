package com.tik.zbb.gametest;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GlobalTestReporter;
import net.minecraft.gametest.framework.JUnitLikeTestReporter;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class MixinCompatibilityScenarios
{
    public static final String REPORT_FILE_PROPERTY = "zbb.mixinCompatibility.reportFile";
    public static final String BLOCK_AND_ACCESSORS_NAME = "block_and_accessors";
    public static final int BLOCK_AND_ACCESSORS_MAX_TICKS = 40;
    public static final String FALLING_BLOCK_NAME = "falling_block";
    public static final int FALLING_BLOCK_MAX_TICKS = 120;
    public static final String NEAREST_TARGET_NAME = "nearest_target_through_wall";
    public static final int NEAREST_TARGET_MAX_TICKS = 40;
    public static final String CONTINUE_TARGET_NAME = "continue_target_through_wall";
    public static final int CONTINUE_TARGET_MAX_TICKS = 40;

    public static final Scenario BLOCK_AND_ACCESSORS = new Scenario(
            BLOCK_AND_ACCESSORS_NAME, BLOCK_AND_ACCESSORS_MAX_TICKS, MixinCompatibilityGameTests::blockAndAccessors);
    public static final Scenario FALLING_BLOCK = new Scenario(
            FALLING_BLOCK_NAME, FALLING_BLOCK_MAX_TICKS, MixinCompatibilityGameTests::fallingBlock);
    public static final Scenario NEAREST_TARGET = new Scenario(
            NEAREST_TARGET_NAME, NEAREST_TARGET_MAX_TICKS, MixinCompatibilityGameTests::nearestTargetThroughWall);
    public static final Scenario CONTINUE_TARGET = new Scenario(
            CONTINUE_TARGET_NAME, CONTINUE_TARGET_MAX_TICKS, MixinCompatibilityGameTests::continueTargetThroughWall);

    public static final List<Scenario> ALL = List.of(
            BLOCK_AND_ACCESSORS, FALLING_BLOCK, NEAREST_TARGET, CONTINUE_TARGET);

    private static final String FORGE_TEST_INSTANCE = """
            {
              "type": "minecraft:function",
              "function": "zbb:%s",
              "environment": "minecraft:default",
              "structure": "minecraft:empty",
              "max_ticks": %d
            }
            """;

    private MixinCompatibilityScenarios() {}

    public static void installXmlReporter()
    {
        String reportFile = System.getProperty(REPORT_FILE_PROPERTY);
        if (reportFile == null)
        {
            throw new IllegalStateException("Missing system property " + REPORT_FILE_PROPERTY);
        }

        try
        {
            GlobalTestReporter.replaceWith(new JUnitLikeTestReporter(Path.of(reportFile).toFile()));
        }
        catch (Exception exception)
        {
            throw new IllegalStateException("Could not create GameTest XML reporter", exception);
        }
    }

    public static void main(String[] args) throws Exception
    {
        if (args[0].equals("verify-junit"))
        {
            verifyJUnitReport(Path.of(args[1]));
            return;
        }

        Path output = Path.of(args[1]);
        Files.createDirectories(output);
        for (Scenario scenario : ALL)
        {
            Files.writeString(output.resolve(scenario.id + ".json"),
                    FORGE_TEST_INSTANCE.formatted(scenario.id, scenario.maxTicks));
        }
    }

    private static void verifyJUnitReport(Path reportFile) throws Exception
    {
        if (!Files.isRegularFile(reportFile))
        {
            throw new AssertionError("GameTest runner did not create " + reportFile);
        }

        var document = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(reportFile.toFile());
        NodeList testCases = document.getElementsByTagName("testcase");
        List<String> missing = new ArrayList<>();

        for (Scenario scenario : ALL)
        {
            Element match = null;
            for (int index = 0; index < testCases.getLength(); index++)
            {
                Element candidate = (Element) testCases.item(index);
                if (scenario.matchesReportName(candidate.getAttribute("name")))
                {
                    if (match != null)
                    {
                        throw new AssertionError("Duplicate GameTest result for " + scenario.id());
                    }
                    match = candidate;
                }
            }

            if (match == null)
            {
                missing.add(scenario.id());
            }
            else if (match.getElementsByTagName("failure").getLength() != 0
                    || match.getElementsByTagName("error").getLength() != 0)
            {
                throw new AssertionError("GameTest failed: " + match.getAttribute("name"));
            }
        }

        if (!missing.isEmpty())
        {
            throw new AssertionError("Missing GameTest results: " + missing);
        }
    }

    public record Scenario(String id, int maxTicks, Consumer<GameTestHelper> test)
    {
        public void run(GameTestHelper helper)
        {
            test.accept(helper);
        }

        private boolean matchesReportName(String reportName)
        {
            return reportName.equals("zbb:" + id) || reportName.endsWith("_" + id);
        }
    }
}
