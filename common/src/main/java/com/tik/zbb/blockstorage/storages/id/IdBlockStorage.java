package com.tik.zbb.blockstorage.storages.id;

import com.tik.zbb.blockstorage.BaseBlockStorage;
import net.minecraft.server.level.ServerLevel;

public class IdBlockStorage extends BaseBlockStorage<IdBlockStorageEntry>
{
    @Override
    protected boolean isExpired(ServerLevel level, long posKey, IdBlockStorageEntry entry, long now, long ttlTicks)
    {
        return false;
    }
}
