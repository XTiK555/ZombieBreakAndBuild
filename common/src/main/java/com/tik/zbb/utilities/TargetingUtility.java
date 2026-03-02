package com.tik.zbb.utilities;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.attributes.Attribute;
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

        // 1) не креатив/спектатор (важно для player)
        if (candidate instanceof Player p && !EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(p))
        {
            return false;
        }

        // 2) команды/союзники и прочее ванильное "может ли моб атаковать"
        // Обычно внутри есть куча правильных проверок.
        if (!mob.canAttack(candidate)) return false;

        // 3) Нейтралы: если моб нейтральный, не даём goal'у "делать его агрессивным".
        // Важно: это как раз предотвращает "железный голем начнет бить игрока просто потому что увидел".
        // (в зависимости от класса моба, может быть NeutralMob или своя логика)
        if (mob instanceof NeutralMob neutral)
        {
            // Если нейтрал не зол на этого кандидата - не таргетим.
            // (в некоторых версиях есть isAngryAt / isAngry; подстрой под свою версию)
            if (!neutral.isAngryAt(candidate, level)) return false;
        }

        // 4) TargetingConditions — ванильный комбат-фильтр.
        // Тут дистанция и LOS сидят внутри, поэтому мы их настраиваем.
        AttributeInstance followRangeAttribute = mob.getAttribute(Attributes.FOLLOW_RANGE);
        double followRange = followRangeAttribute != null ? followRangeAttribute.getValue() : Double.MAX_VALUE;
        double range = ignoreDistance ? Double.MAX_VALUE : followRange;

        TargetingConditions cond = TargetingConditions.forCombat().range(range);
        if (ignoreLineOfSight) cond = cond.ignoreLineOfSight();

        // Важно: этот тест учитывает кучу ванильных нюансов (невидимость, invuln, etc) в зависимости от версии.
        return cond.test(level, mob, candidate);
    }
}
