package com.tik.zbb.mixin;

import com.tik.zbb.config.ConfigManager;
import com.tik.zbb.config.ConfigSnapshot;
import com.tik.zbb.mixin.accessor.TargetingConditionsAccessor;
import com.tik.zbb.utilities.ShouldApplyToMobUtility;
import com.tik.zbb.utilities.TargetVisibilityThroughBlocksUtility;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NearestAttackableTargetGoal.class)
public abstract class NATGoalMixin extends TargetGoal
{
    protected NATGoalMixin(Mob mob, boolean mustSee)
    {
        super(mob, mustSee);
    }

    @Inject(method = "getTargetConditions", at = @At("RETURN"), cancellable = true)
    private void zbb$getTargetConditions(CallbackInfoReturnable<TargetingConditions> cir)
    {
        ConfigSnapshot configSnapshot = ConfigManager.getConfigSnapshot();

        if (!configSnapshot.game().ai().canNoticeTargetsThroughBlocks()) return;
        if (!ShouldApplyToMobUtility.matchesFullZbbMobFilter(this.mob, configSnapshot)) return;

        TargetingConditions original = cir.getReturnValue();
        TargetingConditions.Selector oldSelector = ((TargetingConditionsAccessor) (Object) original).zbb$getSelector();

        TargetingConditions.Selector combinedSelector = (candidate, level) ->
                (oldSelector == null || oldSelector.test(candidate, level)) &&
                        TargetVisibilityThroughBlocksUtility.canSeeThroughSolidBlocks(
                                this.mob,
                                candidate,
                                configSnapshot.game().ai().noticeTargetsThroughBlocksLimit()
                        );

        cir.setReturnValue(original.copy()
                .ignoreLineOfSight()
                .selector(combinedSelector)
        );
    }
}
