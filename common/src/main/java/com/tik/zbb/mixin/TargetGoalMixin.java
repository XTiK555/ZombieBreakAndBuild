package com.tik.zbb.mixin;

import com.tik.zbb.config.ConfigManager;
import com.tik.zbb.utilities.ShouldApplyToMobUtility;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
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
    @Shadow
    @Final
    protected boolean mustSee;

    @Redirect(method = "canContinueToUse", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/ai/goal/target/TargetGoal;mustSee:Z"))
    private boolean zbb$redirectMustSee(TargetGoal instance)
    {
        if (ConfigManager.getConfigSnapshot().data().ai.canContinueSeeingTargetsThroughBlocks && ShouldApplyToMobUtility.matchesZbbMobFilter(mob, ConfigManager.getConfigSnapshot().data()))
        {
            return false;
        }

        return this.mustSee;
    }
}
