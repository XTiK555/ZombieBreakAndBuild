package com.tik.zbb.config.schema;

import java.util.ArrayList;
import java.util.List;

public final class ConfigRepairReport
{
    private final List<String> entries = new ArrayList<>();

    public void repaired(ConfigPath path, Object originalValue, Object fixedValue, String reason)
    {
        entries.add(path + ": " + reason + " (" + originalValue + " -> " + fixedValue + ")");
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
