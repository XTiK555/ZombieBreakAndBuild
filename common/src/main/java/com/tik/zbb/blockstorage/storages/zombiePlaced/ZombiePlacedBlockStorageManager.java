package com.tik.zbb.blockstorage.storages.zombiePlaced;

import com.tik.zbb.ai.action.actions.build.BuildAction;
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
}
