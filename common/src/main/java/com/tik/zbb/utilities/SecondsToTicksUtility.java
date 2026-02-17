package com.tik.zbb.utilities;

public final class SecondsToTicksUtility
{
    private static final double TICKS_PER_SECOND = 20.0;

    private SecondsToTicksUtility()
    {
        // Disable instance creation
    }

    public static long toTicks(double seconds)
    {
        return (long) Math.ceil(seconds * TICKS_PER_SECOND);
    }

    public static long toTicks(double seconds, long minTicks)
    {
        return Math.max(minTicks, toTicks(seconds));
    }
}
