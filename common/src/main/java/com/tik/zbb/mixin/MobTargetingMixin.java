package com.tik.zbb.mixin;

import com.tik.zbb.config.ConfigData;
import com.tik.zbb.config.ConfigManager;
import com.tik.zbb.mixin.accessor.TargetGoalAccessor;
import com.tik.zbb.utilities.FindAnyTargetInRangeUtility;
import com.tik.zbb.utilities.ShouldApplyToMobUtility;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Predicate;

@Mixin(NearestAttackableTargetGoal.class)
public abstract class MobTargetingMixin
{
    @Shadow
    protected TargetingConditions targetConditions;

    @Final
    @Shadow
    protected Class<? extends LivingEntity> targetType;

    @Unique
    private TargetingConditions zbb$prevTargetConditions;

    @Inject(method = "<init>(Lnet/minecraft/world/entity/Mob;Ljava/lang/Class;IZZLjava/util/function/Predicate;)V", at = @At("RETURN"))
    private void zbb$ignoreLineOfSight(Mob mob, Class<? extends LivingEntity> targetType, int randomInterval, boolean mustSee, boolean mustReach, Predicate<LivingEntity> targetPredicate, CallbackInfo ci)
    {
        ConfigData config = ConfigManager.getConfigData();

        if (this.targetConditions == null) return;
        if (!ShouldApplyToMobUtility.shouldSeeTargetsThroughWalls(mob, config)) return;

        this.targetConditions = this.targetConditions.copy().ignoreLineOfSight();
    }

    @Inject(method = "findTarget", at = @At("HEAD"))
    private void zbb$expandPlayerRange_head(CallbackInfo ci)
    {
        ConfigData config = ConfigManager.getConfigData();
        Mob mob = ((TargetGoalAccessor) this).zbb$getMob();

        if (!ShouldApplyToMobUtility.shouldIgnorePlayerTargetRange(mob, config)) return;
        if (!(this.targetType == Player.class) && !(this.targetType == ServerPlayer.class)) return;
        if (!(mob.level() instanceof ServerLevel)) return;
        if (this.targetConditions == null) return;

        double followRange = mob.getAttributeValue(Attributes.FOLLOW_RANGE);

        boolean hasOtherTargets = FindAnyTargetInRangeUtility.hasAnyTargetInRange(
                mob,
                followRange,
                tt -> tt == Player.class || tt == ServerPlayer.class,
                e -> true
        );
        if (hasOtherTargets) return;

        Player nearestPlayer = zbb$findNearestPlayer(mob.level().players(), mob);
        if (nearestPlayer == null) return;

        double distSqr = nearestPlayer.distanceToSqr(mob);
        double range = Math.sqrt(distSqr) + 1.0D;

        this.zbb$prevTargetConditions = this.targetConditions;

        this.targetConditions = this.targetConditions
                .copy()
                .range(range)
                .ignoreLineOfSight();
    }

    @Inject(method = "findTarget", at = @At("RETURN"))
    private void zbb$expandPlayerRange_return(CallbackInfo ci)
    {
        if (this.zbb$prevTargetConditions != null)
        {
            this.targetConditions = this.zbb$prevTargetConditions;
            this.zbb$prevTargetConditions = null;
        }
    }

    @Unique
    private Player zbb$findNearestPlayer(List<? extends Player> players, Mob mob)
    {
        Player best = null;
        double bestDist = Double.POSITIVE_INFINITY;

        for (Player player : players)
        {
            if (!(player.isAlive() && !player.isSpectator() && !player.isCreative())) continue;

            double d = player.distanceToSqr(mob);
            if (d < bestDist)
            {
                bestDist = d;
                best = player;
            }
        }
        return best;
    }
}