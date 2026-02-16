package com.tik.zbb.mixin;

import com.tik.zbb.utilities.IMobAccessor;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Mob.class)
public abstract class MobAccessor implements IMobAccessor
{
    @Override
    @Accessor("goalSelector")
    public abstract GoalSelector zbb_getGoalSelector();

    @Override
    @Accessor("targetSelector")
    public abstract GoalSelector zbb_getTargetSelector();
}
