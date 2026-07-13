package com.tik.zbb.ai.action.actions.build;

import com.tik.zbb.config.ConfigManager;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.SoundType;
import org.greenrobot.eventbus.Subscribe;

public class BuildVisual
{
    @Subscribe
    public void onAnyBlockPlaced(BuildAction.OnAnyBlockPlacedEvent event)
    {
        boolean buildBlockSound = ConfigManager.getConfigSnapshot().game().visualEffects().buildBlockSound();
        SoundType soundType = event.placedState().getSoundType();

        if (buildBlockSound)
            event.level().playSound(null, event.pos(), soundType.getPlaceSound(), SoundSource.BLOCKS, soundType.volume, soundType.pitch);
    }
}
