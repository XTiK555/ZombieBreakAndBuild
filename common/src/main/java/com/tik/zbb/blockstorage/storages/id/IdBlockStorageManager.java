package com.tik.zbb.blockstorage.storages.id;

import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public class IdBlockStorageManager
{
    private final IdBlockStorage idBlockStorage = new IdBlockStorage();
    private final IntArrayFIFOQueue freeIds = new IntArrayFIFOQueue();

    private int nextId = -1;

    public int getOrCreate(ServerLevel level, BlockPos pos)
    {
        IdBlockStorageEntry existing = idBlockStorage.get(level, pos);
        if (existing != null)
        {
            return existing.id();
        }

        int newId;

        if (!freeIds.isEmpty())
        {
            newId = freeIds.dequeueInt();
        }
        else
        {
            if (nextId == Integer.MIN_VALUE) throw new IllegalStateException("IdBlockStorageManager exhausted all negative block IDs");

            newId = nextId--;
        }

        idBlockStorage.put(level, pos.immutable(), new IdBlockStorageEntry(newId));
        return newId;
    }

    public Integer getId(ServerLevel level, BlockPos pos)
    {
        IdBlockStorageEntry entry = idBlockStorage.get(level, pos);
        return entry == null ? null : entry.id();
    }

    public void release(ServerLevel level, BlockPos pos, int id)
    {
        Integer realId = getId(level, pos);

        if (realId == null) return;
        if (realId.intValue() != id) return;

        idBlockStorage.remove(level, pos);
        freeIds.enqueue(id);
    }
}
