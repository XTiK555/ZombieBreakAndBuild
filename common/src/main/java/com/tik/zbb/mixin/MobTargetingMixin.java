package com.tik.zbb.mixin;

import com.tik.zbb.Constants;
import com.tik.zbb.config.ConfigData;
import com.tik.zbb.config.ConfigManager;
import com.tik.zbb.mixin.accessor.GoalSelectorAccessor;
import com.tik.zbb.mixin.accessor.MobAccessor;
import com.tik.zbb.mixin.accessor.NearestAttackableTargetGoalAccessor;
import com.tik.zbb.mixin.accessor.TargetGoalAccessor;
import com.tik.zbb.utilities.ShouldApplyToMobUtility;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Set;

@Mixin(NearestAttackableTargetGoal.class)
public abstract class MobTargetingMixin
{
    @Shadow
    protected TargetingConditions targetConditions;

    @Shadow
    @Final
    protected Class<?> targetType;

    @Unique
    private static final boolean ZBB_DEBUG = true;

    @Inject(
            method = "<init>(Lnet/minecraft/world/entity/Mob;Ljava/lang/Class;IZZLnet/minecraft/world/entity/ai/targeting/TargetingConditions$Selector;)V",
            at = @At("RETURN")
    )
    private void zbb$ignoreLineOfSight(
            Mob mob,
            Class<? extends LivingEntity> targetType,
            int interval,
            boolean mustSee,
            boolean mustReach,
            TargetingConditions.Selector selector,
            CallbackInfo ci
    )
    {
        ConfigData config = ConfigManager.getConfigData();

        if (targetConditions == null) return;
        if (!ShouldApplyToMobUtility.shouldSeeTargetsThroughWalls(mob, config)) return;

        this.targetConditions = this.targetConditions.copy().ignoreLineOfSight();
    }

    @Inject(method = "getTargetConditions", at = @At("HEAD"), cancellable = true)
    private void zbb$expandPlayerRange(CallbackInfoReturnable<TargetingConditions> cir)
    {
        ConfigData config = ConfigManager.getConfigData();

        // IMPORTANT casts
        Mob mob = ((TargetGoalAccessor) (Object) this).zbb$getMob();

        double follow = mob.getAttributeValue(Attributes.FOLLOW_RANGE);
        if (!(follow > 0)) follow = 16.0D; // fallback

        boolean isPlayerGoal = (this.targetType == Player.class || this.targetType == ServerPlayer.class);

        if (ZBB_DEBUG && (mob.tickCount % 100 == 0) && isPlayerGoal)
        {
            Constants.LOG.warn("[ZBB] getTargetConditions mob={} follow={} apply={} playerGoal={}",
                    mob.getType().toString(), follow,
                    ShouldApplyToMobUtility.shouldIgnorePlayerTargetRange(mob, config),
                    true
            );
        }

        if (!ShouldApplyToMobUtility.shouldIgnorePlayerTargetRange(mob, config)) return;
        if (!isPlayerGoal) return;
        if (!(mob.level() instanceof ServerLevel)) return;

        boolean hasOtherTargets = zbb$hasAnyTargetInRange(mob, follow);

        if (ZBB_DEBUG && (mob.tickCount % 100 == 0))
        {
            Constants.LOG.warn("[ZBB] mob={} hasOtherTargetsInFollow={} currentTarget={} ",
                    mob.getType().toString(),
                    hasOtherTargets,
                    (mob.getTarget() == null ? "null" : mob.getTarget().getType().toString())
            );
        }

        if (hasOtherTargets) return;

        Player nearestPlayer = zbb$findNearestPlayer(mob.level().players(), mob);
        if (nearestPlayer == null)
        {
            if (ZBB_DEBUG && (mob.tickCount % 100 == 0))
                Constants.LOG.warn("[ZBB] mob={} nearestPlayer=null", mob.getType().toString());
            return;
        }

        double distSqr = nearestPlayer.distanceToSqr(mob);
        double range = Math.sqrt(distSqr) + 1.0D;

        if (ZBB_DEBUG && (mob.tickCount % 100 == 0))
        {
            Constants.LOG.warn("[ZBB] mob={} expandingPlayerRange to {} (nearestPlayerDist={})",
                    mob.getType().toString(), range, Math.sqrt(distSqr));
        }

        cir.setReturnValue(this.targetConditions.range(range));
    }

    @Unique
    private static boolean zbb$hasAnyTargetInRange(Mob mob, double follow)
    {
        if (!(mob.level() instanceof ServerLevel sl)) return false;

        AABB box = mob.getBoundingBox().inflate(follow, follow, follow);

        GoalSelector targetSelector = ((MobAccessor) (Object) mob).zbb$getTargetSelector();
        Set<WrappedGoal> wrappedGoals = ((GoalSelectorAccessor) (Object) targetSelector).zbb$getAvailableGoals();

        int checkedNatg = 0;

        for (WrappedGoal wrappedGoal : wrappedGoals)
        {
            Goal g = wrappedGoal.getGoal();
            if (!(g instanceof NearestAttackableTargetGoal<?> natg)) continue;

            checkedNatg++;

            // IMPORTANT casts
            Class<?> tt = ((NearestAttackableTargetGoalAccessor) (Object) natg).zbb$getTargetType();

            // skip players
            if (tt == Player.class || tt == ServerPlayer.class) continue;

            @SuppressWarnings("unchecked")
            Class<? extends LivingEntity> livingClass = (Class<? extends LivingEntity>) tt;

            List<? extends LivingEntity> list = mob.level().getEntitiesOfClass(livingClass, box, e -> true);

            // IMPORTANT: conditions must be from THAT goal, not from this (player) goal
            TargetingConditions cond = ((NearestAttackableTargetGoalAccessor) (Object) natg)
                    .zbb$getTargetConditions()
                    .range(follow);

            for (LivingEntity e : list)
            {
                if (e == mob || !e.isAlive()) continue;

                if (cond.test(sl, mob, e))
                {
                    if (ZBB_DEBUG && (mob.tickCount % 100 == 0))
                    {
                        Constants.LOG.warn("[ZBB] mob={} foundOtherTarget type={} viaGoalTargetType={}",
                                mob.getType().toString(),
                                e.getType().toString(),
                                tt.getName()
                        );
                    }
                    return true;
                }
            }
        }

        if (ZBB_DEBUG && (mob.tickCount % 100 == 0))
        {
            Constants.LOG.warn("[ZBB] mob={} checkedNatgGoals={} noneFoundInFollowRange",
                    mob.getType().toString(), checkedNatg);
        }

        return false;
    }

    @Unique
    private Player zbb$findNearestPlayer(List<? extends Player> players, Mob mob)
    {
        Player best = null;
        double bestDist = Double.POSITIVE_INFINITY;

        for (Player player : players)
        {
            if (!zbb$isValidPlayer(player)) continue;

            double d = player.distanceToSqr(mob);
            if (d < bestDist)
            {
                bestDist = d;
                best = player;
            }
        }
        return best;
    }

    @Unique
    private boolean zbb$isValidPlayer(Player player)
    {
        return player.isAlive() && !player.isSpectator() && !player.isCreative();
    }
}
