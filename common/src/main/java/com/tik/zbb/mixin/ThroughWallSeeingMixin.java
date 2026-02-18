package com.tik.zbb.mixin;

import com.tik.zbb.config.ConfigData;
import com.tik.zbb.config.ConfigManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NearestAttackableTargetGoal.class)
public abstract class ThroughWallSeeingMixin
{
    @Shadow
    protected TargetingConditions targetConditions;

    @Inject(method = "<init>(Lnet/minecraft/world/entity/Mob;Ljava/lang/Class;IZZLnet/minecraft/world/entity/ai/targeting/TargetingConditions$Selector;)V", at = @At("RETURN"))
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
        boolean isZombie = mob instanceof Zombie || mob instanceof Drowned || mob instanceof Husk || mob instanceof ZombieVillager;

        if (!(mob.level() instanceof ServerLevel)) return;
        if (this.targetConditions == null) return;
        if (!config.isCanSeeTargetsThroughBlocks) return;
        if (mob.getType().getCategory() != MobCategory.MONSTER) return;
        if (!config.isApplyingToAllHostiles && !isZombie) return;

        this.targetConditions = this.targetConditions.copy().ignoreLineOfSight();
    }
}
