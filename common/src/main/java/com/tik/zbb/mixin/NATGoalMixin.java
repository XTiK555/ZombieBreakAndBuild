package com.tik.zbb.mixin;

import com.tik.zbb.config.ConfigManager;
import com.tik.zbb.config.ConfigSnapshot;
import com.tik.zbb.mixin.accessor.TargetingConditionsAccessor;
import com.tik.zbb.utilities.ShouldApplyToMobUtility;
import com.tik.zbb.utilities.TargetVisibilityThroughBlocksUtility;
import net.minecraft.world.entity.LivingEntity;
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

import java.util.function.Predicate;

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

        ConfigSnapshot configSnapshot = ConfigManager.getConfigSnapshot();

        if (configSnapshot.data().ai.canNoticeTargetsThroughBlocks && ShouldApplyToMobUtility.matchesZbbMobFilter(this.mob, configSnapshot))
        {
            Predicate<LivingEntity> oldSelector =
                    ((TargetingConditionsAccessor) (Object) this.zbb$originalTargetConditions).zbb$getSelector();

            Predicate<LivingEntity> combinedSelector = (candidate) ->
                    (oldSelector == null || oldSelector.test(candidate)) &&
                            TargetVisibilityThroughBlocksUtility.canSeeThroughSolidBlocks(
                                    this.mob,
                                    candidate,
                                    configSnapshot.data().ai.noticeTargetsThroughBlocksLimit
                            );

            this.targetConditions = this.zbb$originalTargetConditions.copy()
                    .ignoreLineOfSight()
                    .selector(combinedSelector);
        }
        else
        {
            this.targetConditions = this.zbb$originalTargetConditions;
        }
    }
}
