package com.tik.zbb.utilities;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;

public class TargetingUtility
{
    public static boolean passesVanillaChecks(Mob mob, LivingEntity candidate, boolean ignoreLineOfSight, boolean ignoreDistance)
    {
        if (candidate == null || !candidate.isAlive()) return false;
        if (!(mob.level() instanceof ServerLevel level)) return false;

        if (candidate instanceof Player p && !EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(p))
        {
            return false;
        }

        if (!mob.canAttack(candidate)) return false;

        if (mob instanceof NeutralMob neutral)
        {
            if (!neutral.isAngryAt(candidate, level)) return false;
        }

        AttributeInstance followRangeAttribute = mob.getAttribute(Attributes.FOLLOW_RANGE);
        double followRange = followRangeAttribute != null ? followRangeAttribute.getValue() : Double.MAX_VALUE;
        double range = ignoreDistance ? Double.MAX_VALUE : followRange;

        TargetingConditions cond = TargetingConditions.forCombat().range(range);
        if (ignoreLineOfSight) cond = cond.ignoreLineOfSight();

        return cond.test(level, mob, candidate);
    }
}
