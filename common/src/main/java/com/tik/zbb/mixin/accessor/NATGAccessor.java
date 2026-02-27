package com.tik.zbb.mixin.accessor;

import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(NearestAttackableTargetGoal.class)
public interface NATGAccessor
{
    @Accessor("targetType")
    Class<?> zbb$getTargetType();

    @Accessor("targetConditions")
    TargetingConditions zbb$getTargetConditions();
}
