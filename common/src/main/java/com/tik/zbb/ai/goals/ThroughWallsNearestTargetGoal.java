package com.tik.zbb.ai.goals;

import com.tik.zbb.config.ConfigManager;
import com.tik.zbb.config.ConfigSnapshot;
import com.tik.zbb.mixin.accessor.NATGAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;

import java.util.List;

public class ThroughWallsNearestTargetGoal extends NearestAttackableTargetGoal<LivingEntity>
{
    private final List<NearestAttackableTargetGoal<?>> vanillaTargetGoals;
    private ConfigSnapshot configSnapshot;

    public ThroughWallsNearestTargetGoal(Mob mob, List<NearestAttackableTargetGoal<?>> vanillaTargetGoals)
    {
        super(mob, LivingEntity.class, true);

        this.configSnapshot = ConfigManager.getConfigSnapshot();
        this.vanillaTargetGoals = vanillaTargetGoals;

        this.targetConditions = TargetingConditions.forCombat()
                .ignoreLineOfSight()
                .range(configSnapshot.data().targetSearchRadius)
                .selector((target, serverLevel) -> isAllowedByVanillaGoals(serverLevel, mob, target));
    }

    private boolean isAllowedByVanillaGoals(ServerLevel level, Mob self, LivingEntity target)
    {
        if (target == self) return false;
        if (!target.isAlive()) return false;

        double range = configSnapshot.data().targetSearchRadius;

        for (NearestAttackableTargetGoal<?> g : vanillaTargetGoals)
        {
            var acc = (NATGAccessor) (Object) g;

            Class<?> tt = acc.zbb$getTargetType();
            if (tt == null) continue;
            if (!tt.isInstance(target)) continue;

            TargetingConditions cond = acc.zbb$getTargetConditions();
            if (cond == null) continue;

            TargetingConditions adjusted = cond.copy()
                    .ignoreLineOfSight()
                    .range(range);

            if (adjusted.test(level, self, target))
                return true;
        }

        return false;
    }

    @Override
    public boolean canUse()
    {
        if (!configSnapshot.data().alwaysSeeNearestPlayer) return false;
        if (!(mob.level() instanceof ServerLevel sl)) return false;

        return super.canUse();
    }
}
