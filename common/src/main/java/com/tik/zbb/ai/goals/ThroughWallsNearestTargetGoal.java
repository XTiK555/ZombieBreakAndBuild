package com.tik.zbb.ai.goals;

import com.tik.zbb.config.ConfigManager;
import com.tik.zbb.mixin.accessor.NATGAccessor;
import com.tik.zbb.utilities.TargetingUtility;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;

import java.util.List;

public class ThroughWallsNearestTargetGoal extends NearestAttackableTargetGoal<LivingEntity>
{
    private final List<NearestAttackableTargetGoal<?>> vanillaTargetGoals;
    private double cachedRange = -1;
    private final java.util.IdentityHashMap<NearestAttackableTargetGoal<?>, TargetingConditions> adjustedCache = new java.util.IdentityHashMap<>();

    public ThroughWallsNearestTargetGoal(Mob mob, List<NearestAttackableTargetGoal<?>> vanillaTargetGoals)
    {
        super(mob, LivingEntity.class, false);

        this.vanillaTargetGoals = vanillaTargetGoals;

        rebuildIfNeeded();
    }

    @Override
    public boolean canUse()
    {
        if (!ConfigManager.getConfigSnapshot().data().ai.canSeeTargetsThroughBlocks)
        {
            return false;
        }

        if (!(mob.level() instanceof ServerLevel))
        {
            return false;
        }

        rebuildIfNeeded();
        return super.canUse();
    }

    private void rebuildIfNeeded()
    {
        double range = mob.getAttributeValue(Attributes.FOLLOW_RANGE);

        if (range == cachedRange && !adjustedCache.isEmpty())
        {
            return;
        }

        cachedRange = range;
        adjustedCache.clear();

        for (NearestAttackableTargetGoal<?> goal : vanillaTargetGoals)
        {
            var acc = (NATGAccessor) (Object) goal;
            TargetingConditions cond = acc.zbb$getTargetConditions();
            if (cond == null) continue;

            TargetingConditions adjusted = cond.copy().ignoreLineOfSight().range(range);
            adjustedCache.put(goal, adjusted);
        }

        this.targetConditions = TargetingConditions.forCombat()
                .ignoreLineOfSight()
                .range(range)
                .selector((target) ->
                        TargetingUtility.passesVanillaChecks(mob, target, true, true)
                                && isAllowedByVanillaGoals(mob, target));
    }

    private boolean isAllowedByVanillaGoals(Mob self, LivingEntity target)
    {
        if (target == self) return false;

        for (NearestAttackableTargetGoal<?> goal : vanillaTargetGoals)
        {
            var acc = (NATGAccessor) (Object) goal;

            Class<?> tt = acc.zbb$getTargetType();
            if (tt == null || !tt.isInstance(target)) continue;

            TargetingConditions adjusted = adjustedCache.get(goal);
            if (adjusted != null && adjusted.test(self, target))
            {
                return true;
            }
        }

        return false;
    }
}
