package com.tik.zbb.mixin;

import com.tik.zbb.config.ConfigManager;
import com.tik.zbb.utilities.ShouldApplyToMobUtility;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NearestAttackableTargetGoal.class)
public abstract class NATGoalMixin extends TargetGoal
{
    @Shadow
    protected TargetingConditions targetConditions;

    @Unique
    private TargetingConditions zbb$originalTargetConditions;

    protected NATGoalMixin(Mob mob, boolean mustSee)
    {
        super(mob, mustSee);
    }

    @Inject(method = "canUse", at = @At("HEAD"))
    private void zbb$adjustLineOfSightBeforeCanUse(CallbackInfoReturnable<Boolean> cir)
    {
        if (this.zbb$originalTargetConditions == null)
        {
            this.zbb$originalTargetConditions = this.targetConditions;
        }

        var data = ConfigManager.getConfigSnapshot().data();

        if (data.ai.canSeeTargetsThroughBlocks
                && ShouldApplyToMobUtility.matchesZbbMobFilter(this.mob, data))
        {
            this.targetConditions = this.zbb$originalTargetConditions.copy().ignoreLineOfSight();
        }
        else
        {
            this.targetConditions = this.zbb$originalTargetConditions;
        }
    }
}