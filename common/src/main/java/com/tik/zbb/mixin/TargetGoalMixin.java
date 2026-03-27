package com.tik.zbb.mixin;

import com.tik.zbb.config.ConfigData;
import com.tik.zbb.config.ConfigManager;
import com.tik.zbb.utilities.ShouldApplyToMobUtility;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.sensing.Sensing;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TargetGoal.class)
public abstract class TargetGoalMixin
{
    @Shadow
    @Final
    protected Mob mob;

    @Redirect(method = "canContinueToUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/sensing/Sensing;hasLineOfSight(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean zbb$hasLineOfSight(Sensing sensing, net.minecraft.world.entity.Entity target)
    {
        ConfigData data = ConfigManager.getConfigSnapshot().data();

        if (data.ai.canContinueSeeingTargetsThroughBlocks && ShouldApplyToMobUtility.matchesZbbMobFilter(this.mob, data) && target instanceof LivingEntity)
        {
            return true;
        }

        return sensing.hasLineOfSight(target);
    }
}