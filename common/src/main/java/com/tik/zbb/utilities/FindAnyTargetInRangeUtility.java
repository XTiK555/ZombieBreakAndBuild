package com.tik.zbb.utilities;

import com.tik.zbb.mixin.accessor.GoalSelectorAccessor;
import com.tik.zbb.mixin.accessor.MobAccessor;
import com.tik.zbb.mixin.accessor.NearestAttackableTargetGoalAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public final class FindAnyTargetInRangeUtility
{
    public static boolean hasAnyTargetInRange(Mob mob, double follow,
                                              Predicate<Class<?>> skipTargetType, Predicate<LivingEntity> extraFilter)
    {
        Optional<LivingEntity> target = findAnyTargetInRange(mob, follow, skipTargetType, extraFilter);
        return target.isPresent();
    }

    public static Optional<LivingEntity> findAnyTargetInRange(Mob mob, double follow,
                                                              Predicate<Class<?>> skipTargetType, Predicate<LivingEntity> extraFilter)
    {
        if (!(mob.level() instanceof ServerLevel sl)) return Optional.empty();

        AABB box = mob.getBoundingBox().inflate(follow, follow, follow);

        GoalSelector targetSelector = ((MobAccessor) (Object) mob).zbb$getTargetSelector();
        Set<WrappedGoal> wrappedGoals = ((GoalSelectorAccessor) (Object) targetSelector).zbb$getAvailableGoals();

        for (WrappedGoal wrappedGoal : wrappedGoals)
        {
            Goal g = wrappedGoal.getGoal();
            if (!(g instanceof NearestAttackableTargetGoal<?> natg)) continue;

            Class<?> tt = ((NearestAttackableTargetGoalAccessor) (Object) natg).zbb$getTargetType();
            if (tt == null) continue;
            if (skipTargetType.test(tt)) continue;

            @SuppressWarnings("unchecked")
            Class<? extends LivingEntity> livingClass = (Class<? extends LivingEntity>) tt;

            TargetingConditions cond = ((NearestAttackableTargetGoalAccessor) (Object) natg)
                    .zbb$getTargetConditions()
                    .copy()
                    .range(follow);

            List<? extends LivingEntity> list = mob.level().getEntitiesOfClass(
                    livingClass,
                    box,
                    e -> e != mob && e.isAlive() && extraFilter.test(e)
            );

            for (LivingEntity e : list)
            {
                if (cond.test(mob, e))
                {
                    return Optional.of(e);
                }
            }
        }

        return Optional.empty();
    }
}
