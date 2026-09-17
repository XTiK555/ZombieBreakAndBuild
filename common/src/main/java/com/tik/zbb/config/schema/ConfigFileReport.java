package com.tik.zbb.config.schema;

import java.util.ArrayList;
import java.util.List;

public final class ConfigFileReport
{
    private final List<String> missingValues = new ArrayList<>();
    private final List<String> invalidValues = new ArrayList<>();

    public void missing(ConfigPath path, Object defaultValue)
    {
        missingValues.add(path + ": missing; using default " + defaultValue);
    }

    public void invalid(ConfigPath path, Object originalValue, Object fixedValue, String reason)
    {
        invalidValues.add(path + ": " + reason + " (" + originalValue + " -> " + fixedValue + ")");
    }

    public boolean changed()
    {
        return !missingValues.isEmpty() || !invalidValues.isEmpty();
    }

    public List<String> missingValues()
    {
        return List.copyOf(missingValues);
    }

    public List<String> invalidValues()
    {
        return List.copyOf(invalidValues);
    }
}
