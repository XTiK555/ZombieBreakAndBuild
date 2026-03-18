package com.tik.zbb.ai.goals;

import com.tik.zbb.config.ConfigData;
import com.tik.zbb.config.ConfigManager;
import com.tik.zbb.utilities.TargetingUtility;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;
import java.util.List;

public class AlwaysSeeNearestPlayerGoal extends Goal
{
    private final Mob mob;
    private Player target;

    public AlwaysSeeNearestPlayerGoal(Mob mob)
    {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.TARGET));
    }

    @SuppressWarnings("resource")
    @Override
    public boolean canUse()
    {
        ConfigData config = ConfigManager.getConfigSnapshot().data();

        if (!config.ai.alwaysSeeNearestPlayer) return false;
        if (!(mob.level() instanceof ServerLevel sl)) return false;
        if (mob.getTarget() != null) return false;

        target = findNearestValidPlayer(sl.players());
        return target != null;
    }

    @Override
    public boolean canContinueToUse()
    {
        return false;
    }

    @Override
    public void start()
    {
        if (target != null)
        {
            mob.setTarget(target);
        }
    }

    @Override
    public void stop()
    {
        target = null;
    }

    private Player findNearestValidPlayer(List<ServerPlayer> players)
    {
        Player best = null;
        double bestDist = Double.POSITIVE_INFINITY;

        for (ServerPlayer player : players)
        {
            if (!TargetingUtility.passesVanillaChecks(mob, player, true, true)) continue;

            double d = player.distanceToSqr(mob);
            if (d < bestDist)
            {
                bestDist = d;
                best = player;
            }
        }

        return best;
    }
}
