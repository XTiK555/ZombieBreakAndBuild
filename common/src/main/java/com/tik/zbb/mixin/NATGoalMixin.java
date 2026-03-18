package com.tik.zbb.mixin;

import com.tik.zbb.config.ConfigManager;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NearestAttackableTargetGoal.class)
public class NATGoalMixin
{
    @Shadow
    protected TargetingConditions targetConditions;

    @Unique
    private TargetingConditions zbb$originalTargetConditions;

    @Inject(method = "<init>(Lnet/minecraft/world/entity/Mob;Ljava/lang/Class;IZZLnet/minecraft/world/entity/ai/targeting/TargetingConditions$Selector;)V", at = @At("TAIL"))
    private void zbb$captureOriginalConditions(CallbackInfo ci)
    {
        this.zbb$originalTargetConditions = this.targetConditions;
    }

    @Inject(method = "canUse", at = @At("HEAD"))
    private void zbb$adjustLineOfSightBeforeCanUse(CallbackInfoReturnable<Boolean> cir)
    {
        if (this.zbb$originalTargetConditions == null)
        {
            this.zbb$originalTargetConditions = this.targetConditions;
        }

        if (ConfigManager.getConfigSnapshot().data().ai.canSeeTargetsThroughBlocks)
        {
            this.targetConditions = this.zbb$originalTargetConditions.copy().ignoreLineOfSight();
        }
        else
        {
            this.targetConditions = this.zbb$originalTargetConditions;
        }
    }
}
