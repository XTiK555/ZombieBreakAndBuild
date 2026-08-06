package com.tik.zbb.ai.goals;

import com.tik.zbb.config.ConfigManager;
import com.tik.zbb.config.ConfigSnapshot;
import com.tik.zbb.utilities.ShouldApplyToMobUtility;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;
import java.util.List;

public class AlwaysSeeNearestPlayerGoal extends Goal
{
    private final TargetingConditions targetingConditions = TargetingConditions.forCombat().range(Double.MAX_VALUE).ignoreLineOfSight();
    private final Mob mob;

    private Player target;

    public AlwaysSeeNearestPlayerGoal(Mob mob)
    {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.TARGET));
    }

    @Override
    public boolean canUse()
    {
        ConfigSnapshot configSnapshot = ConfigManager.getConfigSnapshot();

        if (!configSnapshot.game().ai().alwaysSeeNearestPlayer()) return false;
        if (!ShouldApplyToMobUtility.matchesFullZbbMobFilter(mob, configSnapshot)) return false;
        if (mob.getTarget() != null) return false;

        target = findNearestValidPlayer(((ServerLevel) mob.level()).players());
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
        double bestDistanceSq = Double.POSITIVE_INFINITY;

        for (ServerPlayer player : players)
        {
            if (!passesVanillaChecks(mob, player)) continue;

            double distanceSq = player.distanceToSqr(mob);

            if (distanceSq < bestDistanceSq)
            {
                bestDistanceSq = distanceSq;
                best = player;
            }
        }

        return best;
    }

    private boolean passesVanillaChecks(Mob mob, LivingEntity candidate)
    {
        if (candidate == null || !candidate.isAlive()) return false;
        if (!(mob.level() instanceof ServerLevel level)) return false;
        if (candidate instanceof Player p && !EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(p)) return false;
        if (!mob.canAttack(candidate)) return false;
        if (mob instanceof NeutralMob neutral && !neutral.isAngryAt(candidate, level)) return false;

        return targetingConditions.test(level, mob, candidate);
    }
}
