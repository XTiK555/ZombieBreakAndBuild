package com.tik.zbb.goals;

import com.tik.zbb.Config;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.jetbrains.annotations.NotNull;

public class ThroughWallsNearestTargetGoal<T extends LivingEntity> extends NearestAttackableTargetGoal<@NotNull T>
{
    public ThroughWallsNearestTargetGoal(Mob mob, Class<T> targetClass)
    {
        super(mob, targetClass, true);

        this.targetConditions = TargetingConditions.forCombat()
                .ignoreLineOfSight()
                .range(Config.TARGET_SEARCH_RADIUS.get())
                .selector((target) -> isAllowedTarget(mob, target));
    }

    private static boolean isAllowedTarget(Mob self, LivingEntity target)
    {
        if (target == self) return false;

        // don't attack other monsters
        if (target instanceof Mob m)
        {
            return m.getType().getCategory() != MobCategory.MONSTER;
        }

        return true;
    }
}
