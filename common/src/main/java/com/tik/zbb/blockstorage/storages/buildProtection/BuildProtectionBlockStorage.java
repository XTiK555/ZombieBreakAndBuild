package com.tik.zbb.blockstorage.storages.buildProtection;

import com.tik.zbb.blockstorage.BaseBlockStorage;
import net.minecraft.server.level.ServerLevel;

public class BuildProtectionBlockStorage extends BaseBlockStorage<BuildProtectionBlockStorageEntry>
{
    @Override
    protected boolean isExpired(ServerLevel level, long posKey, BuildProtectionBlockStorageEntry entry, long now, long ttlTicks)
    {
        return now - entry.tick() >= ttlTicks;
    }
}
