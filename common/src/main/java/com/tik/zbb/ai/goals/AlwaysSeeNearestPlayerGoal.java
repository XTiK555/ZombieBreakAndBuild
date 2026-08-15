package com.tik.zbb.ai.goals;

import com.tik.zbb.config.ConfigManager;
import com.tik.zbb.config.ConfigSnapshot;
import com.tik.zbb.utilities.ShouldApplyToMobUtility;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

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

        target = findNearestValidPlayer();
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

    private Player findNearestValidPlayer()
    {
        if (!(mob.level() instanceof ServerLevel level)) return null;

        ServerPlayer best = null;
        double bestDistanceSq = Double.POSITIVE_INFINITY;

        for (ServerPlayer player : level.players())
        {
            if (!isValidPlayer(mob, player)) continue;

            double distanceSq = player.distanceToSqr(mob);

            if (distanceSq < bestDistanceSq)
            {
                bestDistanceSq = distanceSq;
                best = player;
            }
        }

        return best;
    }

    private boolean isValidPlayer(Mob mob, ServerPlayer player)
    {
        if (player == null || !player.isAlive()) return false;
        if (!(mob.level() instanceof ServerLevel level)) return false;
        if (!EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(player)) return false;
        if (!mob.canAttack(player)) return false;
        if (mob instanceof NeutralMob neutral && !neutral.isAngryAt(player, level)) return false;

        return targetingConditions.test(level, mob, player);
    }
}