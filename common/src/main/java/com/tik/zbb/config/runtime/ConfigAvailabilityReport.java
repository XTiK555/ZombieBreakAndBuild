package com.tik.zbb.config.runtime;

import com.tik.zbb.config.schema.ConfigPath;

import java.util.ArrayList;
import java.util.List;

public final class ConfigAvailabilityReport
{
    private final List<String> entries = new ArrayList<>();

    public void unavailable(ConfigPath path, Object value, String reason)
    {
        entries.add(path + ": " + reason + " (ignored " + value + ")");
    }

    public boolean hasEntries()
    {
        return !entries.isEmpty();
    }

    public List<String> entries()
    {
        return List.copyOf(entries);
    }
}
