package com.tik.zbb.gametest;

import com.tik.zbb.Constants;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PortabilityGameTestReportVerifier
{
    private PortabilityGameTestReportVerifier() {}

    public static void verify(Path reportFile, PortabilityGameTestScenarios.Suite suite) throws Exception
    {
        if (!Files.isRegularFile(reportFile)) throw new AssertionError("GameTest runner did not create " + reportFile);

        var document = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(reportFile.toFile());
        NodeList testCases = document.getElementsByTagName("testcase");
        Map<String, PortabilityGameTestScenarios.Scenario> expectedNames = new HashMap<>();
        for (var scenario : suite.scenarios())
            for (String reportName : suite.reportNames(scenario)) expectedNames.put(reportName, scenario);

        Set<String> completed = new HashSet<>();
        List<String> unknown = new ArrayList<>();
        for (int index = 0; index < testCases.getLength(); index++)
        {
            Element result = (Element) testCases.item(index);
            String reportName = result.getAttribute("name");
            var scenario = expectedNames.get(reportName);
            if (scenario == null)
            {
                if (reportName.startsWith(Constants.MOD_ID + ":")
                        || reportName.startsWith(Constants.MOD_ID + "_")
                        || reportName.startsWith(PortabilityGameTestScenarios.TEST_MOD_ID + ":")) unknown.add(reportName);
                continue;
            }

            if (!completed.add(scenario.id())) throw new AssertionError("Duplicate GameTest result for " + scenario.id());
            if (result.getElementsByTagName("failure").getLength() != 0
                    || result.getElementsByTagName("error").getLength() != 0)
                throw new AssertionError("GameTest failed: " + reportName);
            if (result.getElementsByTagName("skipped").getLength() != 0)
                throw new AssertionError("GameTest skipped: " + reportName);
        }

        List<String> missing = suite.scenarios().stream().map(PortabilityGameTestScenarios.Scenario::id)
                .filter(id -> !completed.contains(id)).toList();
        if (!missing.isEmpty()) throw new AssertionError("Missing GameTest results: " + missing);
        if (!unknown.isEmpty()) throw new AssertionError("Unknown portability GameTest results: " + unknown);
    }
}
