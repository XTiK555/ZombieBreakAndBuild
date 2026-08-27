package com.tik.zbb.ai.action.actions.breakk;

import com.tik.zbb.config.ConfigGame;
import com.tik.zbb.config.ConfigSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.greenrobot.eventbus.Subscribe;

public class BreakVisual
{
    @Subscribe
    public void onAnyBlockBroken(BreakAction.OnAnyBlockBrokenEvent event)
    {
        playEffects(event.level(), event.pos(), event.oldState(), event.mob(), event.configSnapshot());
    }

    @Subscribe
    public void onAnyBlockHit(BreakAction.OnAnyBlockHit event)
    {
        playEffects(event.level(), event.pos(), event.state(), event.mob(), event.configSnapshot());
    }

    private void playEffects(ServerLevel level, BlockPos pos, BlockState blockState, PathfinderMob mob, ConfigSnapshot configSnapshot)
    {
        level.levelEvent(2001, pos, Block.getId(blockState)); // particles and sound

        mobSwing(mob, configSnapshot.game());
    }

    private void mobSwing(PathfinderMob mob, ConfigGame config)
    {
        if (!config.visualEffects().breakMobSwing()) return;

        mob.swing(InteractionHand.MAIN_HAND);
        mob.swing(InteractionHand.OFF_HAND);
    }
}
