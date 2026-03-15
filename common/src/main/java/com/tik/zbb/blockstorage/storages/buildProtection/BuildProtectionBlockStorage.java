package com.tik.zbb.blockstorage.storages.buildProtection;

import com.tik.zbb.ai.action.actions.build.BuildAction;
import com.tik.zbb.blockstorage.BaseBlockStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.greenrobot.eventbus.Subscribe;

public class BuildProtectionBlockStorage extends BaseBlockStorage<BuildProtectionEntry>
{
    @Subscribe
    public void onAnyBlockPlaced(BuildAction.OnAnyBlockPlacedEvent event)
    {
        addBuildProtectionData(event.level(), event.pos());
    }

    public void addBuildProtectionData(ServerLevel level, BlockPos pos)
    {
        put(level, pos, new BuildProtectionEntry(level.getGameTime()));
    }

    @Override
    protected boolean isExpired(BuildProtectionEntry entry, long now, long ttlTicks)
    {
        return now - entry.tick() > ttlTicks;
    }
}
