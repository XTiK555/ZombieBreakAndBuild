package com.tik.zbb.mixin.accessor.display;

import com.mojang.math.Transformation;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Display.class)
public interface DisplayAccessor
{
    @Invoker("setTransformation")
    void zbb$setTransformation(Transformation transformation);

    @Invoker("setTransformationInterpolationDuration")
    void zbb$setTransformationInterpolationDuration(int duration);

    @Invoker("setTransformationInterpolationDelay")
    void zbb$setTransformationInterpolationDelay(int ticks);
}