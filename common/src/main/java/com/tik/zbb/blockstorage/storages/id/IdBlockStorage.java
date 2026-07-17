package com.tik.zbb.blockstorage.storages.id;

import com.tik.zbb.blockstorage.BaseBlockStorage;
import net.minecraft.server.level.ServerLevel;

public class IdBlockStorage extends BaseBlockStorage<IdBlockStorageEntry, IdBlockStorageEntry>
{
    @Override
    protected IdBlockStorageEntry toStored(ServerLevel level, IdBlockStorageEntry data)
    {
        return data;
    }

    @Override
    protected IdBlockStorageEntry toData(IdBlockStorageEntry stored)
    {
        return stored;
    }
}
