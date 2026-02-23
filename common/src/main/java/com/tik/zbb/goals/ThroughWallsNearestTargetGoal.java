package com.tik.zbb.goals;

import com.tik.zbb.config.ConfigData;
import com.tik.zbb.config.ConfigManager;
import com.tik.zbb.mixin.accessor.NearestAttackableTargetGoalAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;

import java.util.List;

public class ThroughWallsNearestTargetGoal extends NearestAttackableTargetGoal<LivingEntity>
{
    private final List<NearestAttackableTargetGoal<?>> vanillaTargetGoals;

    public ThroughWallsNearestTargetGoal(Mob mob, List<NearestAttackableTargetGoal<?>> vanillaTargetGoals)
    {
        super(mob, LivingEntity.class, true);

        ConfigData config = ConfigManager.getConfigData();
        this.vanillaTargetGoals = vanillaTargetGoals;

        this.targetConditions = TargetingConditions.forCombat()
                .ignoreLineOfSight()
                .range(config.targetSearchRadius)
                .selector((target) -> isAllowedByVanillaGoals(mob, target));
    }

    private boolean isAllowedByVanillaGoals(Mob self, LivingEntity target)
    {
        if (target == self) return false;
        if (!target.isAlive()) return false;

        ConfigData config = ConfigManager.getConfigData();
        double range = config.targetSearchRadius;

        for (NearestAttackableTargetGoal<?> g : vanillaTargetGoals)
        {
            var acc = (NearestAttackableTargetGoalAccessor) (Object) g;

            Class<?> tt = acc.zbb$getTargetType();
            if (tt == null) continue;
            if (!tt.isInstance(target)) continue;

            TargetingConditions cond = acc.zbb$getTargetConditions();
            if (cond == null) continue;

            TargetingConditions adjusted = cond.copy()
                    .ignoreLineOfSight()
                    .range(range);

            if (adjusted.test(self, target))
                return true;
        }

        return false;
    }

    @Override
    public boolean canUse()
    {
        ConfigData config = ConfigManager.getConfigData();

        if (!config.alwaysSeeNearestPlayer) return false;
        if (!(mob.level() instanceof ServerLevel sl)) return false;

        return super.canUse();
    }
}
