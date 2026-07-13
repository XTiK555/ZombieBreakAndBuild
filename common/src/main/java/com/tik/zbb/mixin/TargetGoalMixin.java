package com.tik.zbb.mixin;

import com.tik.zbb.config.ConfigManager;
import com.tik.zbb.config.ConfigSnapshot;
import com.tik.zbb.utilities.ShouldApplyToMobUtility;
import com.tik.zbb.utilities.TargetVisibilityThroughBlocksUtility;
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
        ConfigSnapshot configSnapshot = ConfigManager.getConfigSnapshot();

        if (configSnapshot.game().ai().canContinueSeeingTargetsThroughBlocks() && ShouldApplyToMobUtility.matchesZbbMobFilter(this.mob, configSnapshot) && target instanceof LivingEntity livingTarget)
        {
            return TargetVisibilityThroughBlocksUtility.canSeeThroughSolidBlocks(
                    this.mob,
                    livingTarget,
                    configSnapshot.game().ai().continueSeeingTargetsThroughBlocksLimit()
            );
        }

        return sensing.hasLineOfSight(target);
    }
}
