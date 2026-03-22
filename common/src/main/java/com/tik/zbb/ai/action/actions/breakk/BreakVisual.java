package com.tik.zbb.ai.action.actions.breakk;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.block.Block;
import org.greenrobot.eventbus.Subscribe;

public class BreakVisual
{
    @Subscribe
    public void onAnyBlockBroken(BreakAction.OnAnyBlockBrokenEvent event)
    {
        mobSwing(event.mob());
    }

    @Subscribe
    public void onAnyBlockHit(BreakAction.OnAnyBlockHit event)
    {
        int stage = Math.min(9, (event.totalDamage() * 10) / event.blockHealth());

        event.level().destroyBlockProgress(event.blockId(), event.pos(), stage);
        event.level().levelEvent(2001, event.pos(), Block.getId(event.state())); // particles and sound

        mobSwing(event.mob());
    }

    private void mobSwing(PathfinderMob mob)
    {
        mob.swing(InteractionHand.MAIN_HAND);
        mob.swing(InteractionHand.OFF_HAND);
    }

}
