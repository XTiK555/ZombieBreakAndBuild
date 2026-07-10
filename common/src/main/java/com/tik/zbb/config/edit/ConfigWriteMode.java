package com.tik.zbb.config.edit;

import java.util.Locale;
import java.util.Optional;

public enum ConfigWriteMode
{
    PERSISTENT,
    RUNTIME_ONLY;

    public static Optional<ConfigWriteMode> parse(String raw)
    {
        try
        {
            return Optional.of(ConfigWriteMode.valueOf(raw.toUpperCase(Locale.ROOT)));
        }
        catch (IllegalArgumentException e)
        {
            return Optional.empty();
        }
    }

    public String commandName()
    {
        return name().toLowerCase(Locale.ROOT);
    }
}
