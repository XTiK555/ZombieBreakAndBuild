package com.tik.zbb.gametest;

import java.nio.file.Path;

public final class PortabilityGameTestReportCli
{
    private PortabilityGameTestReportCli() {}

    public static void main(String[] args) throws Exception
    {
        if (args.length != 2) throw new IllegalArgumentException("Expected: <report-path> <suite>");

        PortabilityGameTestReportVerifier.verify(Path.of(args[0]), PortabilityGameTestScenarios.suite(args[1]));
    }
}
