package com.tik.zbb.mixin;

import com.tik.zbb.config.ConfigManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.scores.Team;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TargetGoal.class)
public abstract class TargetGoalMixin
{
    @Shadow
    @Final
    protected Mob mob;

    @Shadow
    @Final
    protected boolean mustSee;

    @Shadow
    protected LivingEntity targetMob;

    @Shadow
    private int unseenTicks;

    @Shadow
    protected abstract double getFollowDistance();

    @Inject(method = "canContinueToUse", at = @At("HEAD"), cancellable = true)
    private void zbb$canContinueWithoutLineOfSight(CallbackInfoReturnable<Boolean> cir)
    {
        if (!this.mustSee || !ConfigManager.getConfigSnapshot().data().ai.canSeeTargetsThroughBlocks)
        {
            return;
        }

        cir.setReturnValue(this.zbb$canContinueIgnoringLineOfSight());
    }

    @Unique
    private boolean zbb$canContinueIgnoringLineOfSight()
    {
        LivingEntity livingentity = this.mob.getTarget();
        if (livingentity == null)
        {
            livingentity = this.targetMob;
        }

        if (livingentity == null)
        {
            return false;
        }
        else if (!this.mob.canAttack(livingentity))
        {
            return false;
        }
        else
        {
            Team team = this.mob.getTeam();
            Team team1 = livingentity.getTeam();
            if (team != null && team1 == team)
            {
                return false;
            }
            else
            {
                double d0 = this.getFollowDistance();
                if (this.mob.distanceToSqr(livingentity) > d0 * d0)
                {
                    return false;
                }
                else
                {
                    this.unseenTicks = 0;
                    this.mob.setTarget(livingentity);
                    return true;
                }
            }
        }
    }
}
