package com.tik.zbb.config;

public final class ConfigSnapshot
{
    private final ConfigData data;
    private final ConfigRuntime runtime;
    private final long version;

    private ConfigSnapshot(ConfigData data, ConfigRuntime runtime, long version)
    {
        this.data = ConfigDataCopier.copy(data);
        this.runtime = runtime;
        this.version = version;
    }

    public static ConfigSnapshot create(ConfigData data, long version)
    {
        return new ConfigSnapshot(data, ConfigRuntime.create(data), version);
    }

    public ConfigData data()
    {
        return ConfigDataCopier.copy(data);
    }

    public ConfigRuntime runtime()
    {
        return runtime;
    }

    public long version()
    {
        return version;
    }
}
