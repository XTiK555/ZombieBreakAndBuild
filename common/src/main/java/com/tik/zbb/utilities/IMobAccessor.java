package com.tik.zbb.utilities;

import net.minecraft.world.entity.ai.goal.GoalSelector;

public interface IMobAccessor
{
    GoalSelector zbb_getGoalSelector();

    GoalSelector zbb_getTargetSelector();
}
