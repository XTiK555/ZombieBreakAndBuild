package com.tik.zbb.ai.action.actions.build;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.SoundType;
import org.greenrobot.eventbus.Subscribe;

public class BuildVisual
{
    @Subscribe
    public void onAnyBlockPlaced(BuildAction.OnAnyBlockPlacedEvent event)
    {
        SoundType soundType = event.state().getSoundType();

        event.level().playSound(null, event.pos(), soundType.getPlaceSound(), SoundSource.BLOCKS, soundType.volume, soundType.pitch);
    }
}
