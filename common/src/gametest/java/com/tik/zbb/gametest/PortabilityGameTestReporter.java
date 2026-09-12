package com.tik.zbb.gametest;

import net.minecraft.gametest.framework.JUnitLikeTestReporter;
import net.minecraft.gametest.framework.TestReporter;

import java.nio.file.Path;

public final class PortabilityGameTestReporter
{
    private PortabilityGameTestReporter() {}

    public static TestReporter xmlReporter()
    {
        String reportFile = System.getProperty(PortabilityGameTestScenarios.REPORT_FILE_PROPERTY);
        if (reportFile == null)
            throw new IllegalStateException("Missing system property " + PortabilityGameTestScenarios.REPORT_FILE_PROPERTY);

        try
        {
            return new JUnitLikeTestReporter(Path.of(reportFile).toFile());
        }
        catch (Exception exception)
        {
            throw new IllegalStateException("Could not create GameTest XML reporter", exception);
        }
    }

}
