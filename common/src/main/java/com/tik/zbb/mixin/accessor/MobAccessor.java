package com.tik.zbb.mixin.accessor;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Mob.class)
public interface MobAccessor
{
    @Accessor("goalSelector")
    GoalSelector zbb$getGoalSelector();

    @Accessor("targetSelector")
    GoalSelector zbb$getTargetSelector();
}
