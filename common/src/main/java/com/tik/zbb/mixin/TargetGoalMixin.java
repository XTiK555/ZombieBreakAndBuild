package com.tik.zbb.mixin;

import com.tik.zbb.config.ConfigManager;
import com.tik.zbb.config.ConfigSnapshot;
import com.tik.zbb.utilities.ShouldApplyToMobUtility;
import com.tik.zbb.utilities.TargetVisibilityThroughBlocksUtility;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TargetGoal.class)
public abstract class TargetGoalMixin
{
    @Shadow
    @Final
    protected Mob mob;

    @ModifyExpressionValue(method = "canContinueToUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/sensing/Sensing;hasLineOfSight(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean zbb$hasLineOfSight(boolean original, @Local LivingEntity target)
    {
        if (original) return true;

        ConfigSnapshot configSnapshot = ConfigManager.getConfigSnapshot();
        if (configSnapshot.game().ai().canContinueSeeingTargetsThroughBlocks()
                && ShouldApplyToMobUtility.matchesFullZbbMobFilter(this.mob, configSnapshot)
                && target != null)
        {
            return TargetVisibilityThroughBlocksUtility.canSeeThroughSolidBlocks(
                    this.mob,
                    target,
                    configSnapshot.game().ai().continueSeeingTargetsThroughBlocksLimit()
            );
        }

        return false;
    }
}
