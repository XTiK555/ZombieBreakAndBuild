package com.tik.zbb.goals;

import com.tik.zbb.config.ConfigData;
import com.tik.zbb.config.ConfigManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;

public class ThroughWallsNearestTargetGoal<T extends LivingEntity> extends NearestAttackableTargetGoal<T>
{
    public ThroughWallsNearestTargetGoal(Mob mob, Class<T> targetClass)
    {
        super(mob, targetClass, true);

        ConfigData config = ConfigManager.getConfigData();

        this.targetConditions = TargetingConditions.forCombat()
                .ignoreLineOfSight()
                .range(config.targetSearchRadius)
                .selector((target, serverLevel) -> isAllowedTarget(mob, target));
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
