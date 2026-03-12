package com.tik.zbb.utilities;

import com.tik.zbb.config.ConfigData;

public class DistanceIntervalUtility
{
    public static double applyDistanceMultiplier(double baseSeconds, double distance, ConfigData configData)
    {
        return baseSeconds * getDistanceMultiplier(distance, configData.balance);
    }

    private static double getDistanceMultiplier(double distance, ConfigData.Balance balance)
    {
        double startDistance = balance.distanceCooldownStartBlocks;
        double maxDistance = balance.distanceCooldownMaxBlocks;
        double maxMultiplier = balance.distanceCooldownMaxMultiplier;

        if (maxMultiplier <= 1.0D)
        {
            return 1.0D;
        }

        if (maxDistance <= startDistance)
        {
            return distance > startDistance ? maxMultiplier : 1.0D;
        }

        if (distance <= startDistance)
        {
            return 1.0D;
        }

        if (distance >= maxDistance)
        {
            return maxMultiplier;
        }

        double progress = (distance - startDistance) / (maxDistance - startDistance);
        return 1.0D + progress * (maxMultiplier - 1.0D);
    }
}
