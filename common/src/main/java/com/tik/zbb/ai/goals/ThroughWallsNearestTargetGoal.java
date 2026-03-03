package com.tik.zbb.ai.goals;

import com.tik.zbb.config.ConfigManager;
import com.tik.zbb.config.ConfigSnapshot;
import com.tik.zbb.mixin.accessor.NATGAccessor;
import com.tik.zbb.utilities.TargetingUtility;
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
    private double cachedRange = -1;
    private final java.util.IdentityHashMap<NearestAttackableTargetGoal<?>, TargetingConditions> adjustedCache = new java.util.IdentityHashMap<>();

    public ThroughWallsNearestTargetGoal(Mob mob, List<NearestAttackableTargetGoal<?>> vanillaTargetGoals)
    {
        super(mob, LivingEntity.class, false);

        this.configSnapshot = ConfigManager.getConfigSnapshot();
        this.vanillaTargetGoals = vanillaTargetGoals;

        rebuildTargetConditions();
    }

    private boolean isAllowedByVanillaGoals(Mob self, LivingEntity target)
    {
        if (target == self) return false;

        rebuildCacheIfNeeded();

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

    @Override
    public boolean canUse()
    {
        ConfigSnapshot snap = ConfigManager.getConfigSnapshot();
        if (snap.version() != configSnapshot.version())
        {
            configSnapshot = snap;
            rebuildTargetConditions();
            cachedRange = -1;
        }

        if (!configSnapshot.data().canSeeTargetsThroughBlocks) return false;
        if (!(mob.level() instanceof ServerLevel)) return false;

        return super.canUse();
    }

    private void rebuildCacheIfNeeded()
    {
        double range = configSnapshot.data().targetSearchRadius;
        if (range == cachedRange && !adjustedCache.isEmpty()) return;

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
    }

    private void rebuildTargetConditions()
    {
        this.targetConditions = TargetingConditions.forCombat()
                .ignoreLineOfSight()
                .range(configSnapshot.data().targetSearchRadius)
                .selector((target) ->
                        TargetingUtility.passesVanillaChecks(mob, target, true, true)
                                && isAllowedByVanillaGoals(mob, target));
    }
}
