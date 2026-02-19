package com.tik.zbb.mixin;

import com.tik.zbb.config.ConfigData;
import com.tik.zbb.config.ConfigManager;
import com.tik.zbb.mixin.accessor.NearestAttackableTargetGoalAccessor;
import com.tik.zbb.utilities.ShouldApplyToMobUtility;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TargetGoal.class)
public abstract class TargetGoalRetargetMixin
{
    @Shadow
    @Final
    protected Mob mob;

    @Unique
    private int zbb$nextRetargetTick = 0;

    @Inject(method = "start", at = @At("TAIL"))
    private void zbb$setRetargetTimer(CallbackInfo ci)
    {
        if (!zbb$shouldAffectThisGoal()) return;
        zbb$nextRetargetTick = mob.tickCount + 20;
    }

    @Inject(method = "canContinueToUse", at = @At("HEAD"), cancellable = true)
    private void zbb$forcePeriodicRetarget(CallbackInfoReturnable<Boolean> cir)
    {
        if (!zbb$shouldAffectThisGoal()) return;

        if (mob.tickCount >= zbb$nextRetargetTick)
        {
            zbb$nextRetargetTick = mob.tickCount + 20;

            LivingEntity cur = mob.getTarget();
            if (cur instanceof Player)
            {
                mob.setTarget(null);
            }

            cir.setReturnValue(false);
        }
    }

    @Unique
    private boolean zbb$shouldAffectThisGoal()
    {
        ConfigData config = ConfigManager.getConfigData();

        if (!ShouldApplyToMobUtility.shouldIgnorePlayerTargetRange(mob, config)) return false;
        if (!((Object) this instanceof NearestAttackableTargetGoal)) return false;

        Class<?> tt = ((NearestAttackableTargetGoalAccessor) this).zbb$getTargetType();
        return tt == Player.class || tt == ServerPlayer.class;
    }
}
