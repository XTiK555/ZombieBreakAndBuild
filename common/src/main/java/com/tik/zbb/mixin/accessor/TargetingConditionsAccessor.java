package com.tik.zbb.mixin.accessor;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.Predicate;

@Mixin(TargetingConditions.class)
public interface TargetingConditionsAccessor
{
    @Accessor("selector")
    Predicate<LivingEntity> zbb$getSelector();
}