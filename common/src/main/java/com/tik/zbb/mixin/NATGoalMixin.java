package com.tik.zbb.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
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
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Predicate;

@Mixin(NearestAttackableTargetGoal.class)
public abstract class NATGoalMixin extends TargetGoal
{
    protected NATGoalMixin(Mob mob, boolean mustSee)
    {
        super(mob, mustSee);
    }

    @ModifyExpressionValue(method = "findTarget", at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/world/entity/ai/goal/target/NearestAttackableTargetGoal;targetConditions:Lnet/minecraft/world/entity/ai/targeting/TargetingConditions;"
    ))
    private TargetingConditions zbb$getTargetConditions(TargetingConditions original)
    {
        ConfigSnapshot configSnapshot = ConfigManager.getConfigSnapshot();

        if (!configSnapshot.game().ai().canNoticeTargetsThroughBlocks()) return original;
        if (!ShouldApplyToMobUtility.matchesFullZbbMobFilter(this.mob, configSnapshot)) return original;

        Predicate<LivingEntity> oldSelector = ((TargetingConditionsAccessor) (Object) original).zbb$getSelector();

        Predicate<LivingEntity> combinedSelector = candidate ->
                (oldSelector == null || oldSelector.test(candidate)) &&
                        TargetVisibilityThroughBlocksUtility.canSeeThroughSolidBlocks(
                                this.mob,
                                candidate,
                                configSnapshot.game().ai().noticeTargetsThroughBlocksLimit()
                        );

        return original.copy()
                .ignoreLineOfSight()
                .selector(combinedSelector);
    }
}
