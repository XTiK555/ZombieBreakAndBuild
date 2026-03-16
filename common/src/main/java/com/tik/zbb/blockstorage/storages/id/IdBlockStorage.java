package com.tik.zbb.blockstorage.storages.id;

import com.tik.zbb.blockstorage.BaseBlockStorage;
import com.tik.zbb.blockstorage.storages.damage.DamageBlockStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.greenrobot.eventbus.Subscribe;

import java.util.concurrent.atomic.AtomicInteger;

public class IdBlockStorage extends BaseBlockStorage<IdBlockStorageEntry>
{
    @Subscribe
    public void onDamageEntryRemoved(DamageBlockStorage.OnBlockDamageProgressRemovedEvent event)
    {
        remove(event.level(), event.pos());
    }

    private final AtomicInteger nextId = new AtomicInteger(1);

    public int getOrCreate(ServerLevel level, BlockPos pos)
    {
        IdBlockStorageEntry existing = get(level, pos);
        if (existing != null)
        {
            return existing.id();
        }

        int newId = nextId.getAndIncrement();
        put(level, pos.immutable(), new IdBlockStorageEntry(newId));
        return newId;
    }

    public Integer getId(ServerLevel level, BlockPos pos)
    {
        IdBlockStorageEntry entry = get(level, pos);
        return entry == null ? null : entry.id();
    }

    @Override
    protected boolean isExpired(IdBlockStorageEntry entry, long now, long ttlTicks)
    {
        return false;
    }
}
