package com.tik.zbb.blockstorage.storages.id;

import com.tik.zbb.blockstorage.BlockStorages;
import com.tik.zbb.blockstorage.storages.damage.DamageBlockStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.greenrobot.eventbus.Subscribe;

import java.util.concurrent.atomic.AtomicInteger;

public class IdBlockStorageManager
{
    private final IdBlockStorage idBlockStorage = new IdBlockStorage();

    private final AtomicInteger nextId = new AtomicInteger(1);

    @Subscribe
    public void onDamageEntryRemoved(DamageBlockStorage.OnRemovedEvent event)
    {
        idBlockStorage.remove(event.level(), event.pos());
    }

    public int getOrCreate(ServerLevel level, BlockPos pos)
    {
        IdBlockStorageEntry existing = idBlockStorage.get(level, pos);
        if (existing != null)
        {
            return existing.id();
        }
        int newId = nextId.getAndIncrement();

        idBlockStorage.put(level, pos.immutable(), new IdBlockStorageEntry(newId));
        return newId;
    }

    public Integer getId(ServerLevel level, BlockPos pos)
    {
        IdBlockStorageEntry entry = idBlockStorage.get(level, pos);
        return entry == null ? null : entry.id();
    }
}
