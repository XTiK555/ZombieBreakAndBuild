package com.tik.zbb.mixin;

import com.tik.zbb.config.ConfigData;
import com.tik.zbb.config.ConfigManager;
import com.tik.zbb.mixin.accessor.TargetingConditionsAccessor;
import com.tik.zbb.utilities.ShouldApplyToMobUtility;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
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

        ConfigData data = ConfigManager.getConfigSnapshot().data();

        if (data.ai.canNoticeTargetsThroughBlocks && ShouldApplyToMobUtility.matchesZbbMobFilter(this.mob, data))
        {
            Predicate<LivingEntity> oldSelector =
                    ((TargetingConditionsAccessor) (Object) this.zbb$originalTargetConditions).zbb$getSelector();

            Predicate<LivingEntity> combinedSelector = (candidate) ->
                    (oldSelector == null || oldSelector.test(candidate)) &&
                            zbb$canNoticeTargetThroughSolidBlocks(
                                    this.mob,
                                    candidate,
                                    data.ai.noticeTargetsThroughBlocksLimit
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

    @Unique
    private static boolean zbb$canNoticeTargetThroughSolidBlocks(Mob mob, LivingEntity target, int maxSolidBlocks)
    {
        if (maxSolidBlocks == 0)
        {
            return true; // infinity
        }

        Level level = mob.level();

        Vec3 from = mob.getEyePosition();
        Vec3 to = target.getEyePosition();

        double distance = from.distanceTo(to);
        if (distance <= 0.0001D)
        {
            return true;
        }

        int steps = Math.max(1, (int) Math.ceil(distance * 5.0D));
        int solidBlocks = 0;

        BlockPos startPos = BlockPos.containing(from);
        BlockPos endPos = BlockPos.containing(to);
        BlockPos lastPos = null;

        for (int i = 1; i < steps; i++)
        {
            double t = (double) i / (double) steps;
            Vec3 point = from.lerp(to, t);
            BlockPos pos = BlockPos.containing(point);

            if (lastPos != null && lastPos.equals(pos))
            {
                continue;
            }

            lastPos = pos;

            if (pos.equals(startPos) || pos.equals(endPos))
            {
                continue;
            }

            BlockState state = level.getBlockState(pos);

            if (!state.isAir() && state.isSolidRender(level, pos))
            {
                solidBlocks++;

                if (solidBlocks > maxSolidBlocks)
                {
                    return false;
                }
            }
        }

        return true;
    }
}