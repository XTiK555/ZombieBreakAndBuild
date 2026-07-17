package com.tik.zbb.ai.action.actions.breakk;

import com.tik.zbb.config.ConfigGame;
import com.tik.zbb.config.ConfigManager;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.block.Block;
import org.greenrobot.eventbus.Subscribe;

public class BreakVisual
{
    @Subscribe
    public void onAnyBlockBroken(BreakAction.OnAnyBlockBrokenEvent event)
    {
        ConfigGame.VisualEffects visualEffects = ConfigManager.getConfigSnapshot().game().visualEffects();

        if (visualEffects.breakMobSwing()) mobSwing(event.mob());
    }

    @Subscribe
    public void onAnyBlockHit(BreakAction.OnAnyBlockHit event)
    {
        ConfigGame.VisualEffects visualEffects = ConfigManager.getConfigSnapshot().game().visualEffects();
        int stage = (int) Math.min(9L, ((long) event.totalDamage() * 10L) / event.blockHealth());

        event.level().destroyBlockProgress(event.blockId(), event.pos(), stage);
        event.level().levelEvent(2001, event.pos(), Block.getId(event.state())); // particles and sound

        if (visualEffects.breakMobSwing()) mobSwing(event.mob());
    }

    private void mobSwing(PathfinderMob mob)
    {
        mob.swing(InteractionHand.MAIN_HAND);
        mob.swing(InteractionHand.OFF_HAND);
    }

}
