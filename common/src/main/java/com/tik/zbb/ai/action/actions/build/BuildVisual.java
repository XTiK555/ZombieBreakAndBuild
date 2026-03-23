package com.tik.zbb.ai.action.actions.build;

import com.tik.zbb.config.ConfigData;
import com.tik.zbb.config.ConfigManager;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.SoundType;
import org.greenrobot.eventbus.Subscribe;

public class BuildVisual
{
    @Subscribe
    public void onAnyBlockPlaced(BuildAction.OnAnyBlockPlacedEvent event)
    {
        ConfigData configData = ConfigManager.getConfigSnapshot().data();
        SoundType soundType = event.placedState().getSoundType();

        if (configData.visualEffects.buildBlockSound)
            event.level().playSound(null, event.pos(), soundType.getPlaceSound(), SoundSource.BLOCKS, soundType.volume, soundType.pitch);
    }
}
