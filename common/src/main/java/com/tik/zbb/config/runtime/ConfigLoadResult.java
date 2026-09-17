package com.tik.zbb.config.runtime;

import com.tik.zbb.config.schema.ConfigFileReport;

public record ConfigLoadResult(
        boolean success,
        boolean saved,
        ConfigFileReport fileReport,
        ConfigAvailabilityReport availabilityReport,
        String message
) {}
