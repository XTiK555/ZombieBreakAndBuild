package com.tik.zbb.goals;

import com.tik.zbb.Config;
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
        if (!Config.ALWAYS_SEE_NEAREST_PLAYER.get()) return false;
        if (!(mob.level() instanceof ServerLevel sl)) return false;
        if (mob.getTarget() != null && mob.getTarget().isAlive() && isValidPlayer(mob.getTarget())) return false;

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
