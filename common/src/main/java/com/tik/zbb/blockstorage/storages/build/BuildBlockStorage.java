package com.tik.zbb.blockstorage.storages.build;

import com.tik.zbb.blockstorage.BaseBlockStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public class BuildBlockStorage extends BaseBlockStorage<BuildEntry>
{
    public void addBuildData(ServerLevel level, BlockPos pos)
    {
        put(level, pos, new BuildEntry(level.getGameTime()));
    }

    @Override
    protected boolean isExpired(BuildEntry entry, long now, long ttlTicks)
    {
        return now - entry.lastTick() > ttlTicks;
    }
}
