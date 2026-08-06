package com.tik.zbb.blockstorage.storages.zombiePlaced;

import com.tik.zbb.ai.action.actions.build.BuildAction;
import com.tik.zbb.config.ConfigGame;
import com.tik.zbb.event.MixinEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.greenrobot.eventbus.Subscribe;

public class ZombiePlacedBlockStorageManager
{
    private final ZombiePlacedBlockStorage storage = new ZombiePlacedBlockStorage();

    @Subscribe
    public void onAnyBlockPlaced(BuildAction.OnAnyBlockPlacedEvent event)
    {
        if (!storageConditions(event.configSnapshot().game())) return;

        storage.put(event.level(), event.pos(), event.placedState());
    }

    @Subscribe
    public void onLevelChunkBlockChanged(MixinEvents.OnLevelChunkBlockChangedEvent event)
    {
        storage.remove(event.level(), event.pos());
    }

    public boolean contains(ServerLevel level, BlockPos pos)
    {
        return storage.contains(level, pos);
    }

    public void remove(ServerLevel level, BlockPos pos)
    {
        storage.remove(level, pos);
    }

    // temp for optimization, may be removed in the future
    private boolean storageConditions(ConfigGame config)
    {
        boolean brokenRestoring = config.blockRestoration().brokenBlocksRestoring();
        boolean builtDisappear = config.blockRestoration().builtBlocksDisappearing();

        return brokenRestoring && !builtDisappear;
    }
}
