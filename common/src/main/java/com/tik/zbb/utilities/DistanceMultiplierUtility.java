package com.tik.zbb.utilities;

public class DistanceMultiplierUtility
{
    public static double applyDistanceMultiplier(double baseSeconds, double distance, double minDistance, double maxDistance, double maxMultiplier)
    {
        return baseSeconds * getDistanceMultiplier(distance, minDistance, maxDistance, maxMultiplier);
    }

    private static double getDistanceMultiplier(double distance, double minDistance, double maxDistance, double maxMultiplier)
    {
        if (maxMultiplier <= 1.0D)
        {
            return 1.0D;
        }

        if (maxDistance <= minDistance)
        {
            return distance > minDistance ? maxMultiplier : 1.0D;
        }

        if (distance <= minDistance)
        {
            return 1.0D;
        }

        if (distance >= maxDistance)
        {
            return maxMultiplier;
        }

        double progress = (distance - minDistance) / (maxDistance - minDistance);
        return 1.0D + progress * (maxMultiplier - 1.0D);
    }
}
