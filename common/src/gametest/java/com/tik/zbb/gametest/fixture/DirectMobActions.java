package com.tik.zbb.gametest.fixture;

import com.tik.zbb.ai.AiTimers;
import com.tik.zbb.ai.action.ActionExecutor;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.PathfinderMob;

public final class DirectMobActions
{
    private final GameTestHelper helper;
    private final ActionExecutor executor;

    private DirectMobActions(GameTestHelper helper, PathfinderMob mob)
    {
        this.helper = helper;
        executor = new ActionExecutor(mob, new AiTimers());
    }

    public static DirectMobActions forMob(GameTestHelper helper, PathfinderMob mob)
    {
        return new DirectMobActions(helper, mob);
    }

    public void build(BlockPos relativePos)
    {
        helper.assertTrue(executor.tryExecuteBuildAction(helper.absolutePos(relativePos)),
                "Direct build action was rejected at " + relativePos);
    }

    public void breakBlock(BlockPos relativePos)
    {
        helper.assertTrue(executor.tryExecuteBreakAction(helper.absolutePos(relativePos)),
                "Direct break action was rejected at " + relativePos);
    }
}
