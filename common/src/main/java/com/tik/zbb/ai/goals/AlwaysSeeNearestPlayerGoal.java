package com.tik.zbb.ai.goals;

import com.tik.zbb.config.ConfigData;
import com.tik.zbb.config.ConfigManager;
import com.tik.zbb.utilities.TargetingUtility;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
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

        if (!config.alwaysSeeNearestPlayer) return false;
        if (!(mob.level() instanceof ServerLevel sl)) return false;
        if (mob.getTarget() != null && isValidPlayer(mob.getTarget())) return false;

        target = findNearestValidPlayer(sl.players());

        return target != null;
    }

    @Override
    public boolean canContinueToUse()
    {
        return isValidPlayer(target) && mob.getTarget() == target;
    }

    @Override
    public void start()
    {
        mob.setTarget(target);
    }

    @Override
    public void stop()
    {
        if (mob.getTarget() == target) mob.setTarget(null);
        target = null;
    }

    private static boolean isValidPlayer(LivingEntity livingEntity)
    {
        return livingEntity instanceof Player && livingEntity.isAlive() && !livingEntity.isSpectator() && !((Player) livingEntity).isCreative();
    }

    private Player findNearestValidPlayer(List<ServerPlayer> players)
    {
        Player best = null;
        double bestDist = Double.POSITIVE_INFINITY;

        for (ServerPlayer p : players)
        {
            if (!TargetingUtility.passesVanillaChecks(mob, p, true, true)) continue;
            if (!isValidPlayer(p)) continue;
            double d = p.distanceToSqr(mob);
            if (d < bestDist)
            {
                bestDist = d;
                best = p;
            }
        }

        return best;
    }
}
