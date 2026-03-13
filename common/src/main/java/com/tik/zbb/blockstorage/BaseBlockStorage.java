package com.tik.zbb.blockstorage;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.Map;
import java.util.WeakHashMap;

public abstract class BaseBlockStorage<TEntry>
{
    protected final Map<ServerLevel, Long2ObjectOpenHashMap<TEntry>> entriesByLevel = new WeakHashMap<>();

    public TEntry get(ServerLevel level, BlockPos pos)
    {
        var map = entriesByLevel.get(level);
        return map != null ? map.get(pos.asLong()) : null;
    }

    public boolean contains(ServerLevel level, BlockPos pos)
    {
        var map = entriesByLevel.get(level);
        return map != null && map.containsKey(pos.asLong());
    }

    public void remove(ServerLevel level, BlockPos pos)
    {
        var map = entriesByLevel.get(level);
        if (map == null) return;

        long key = pos.asLong();
        TEntry removed = map.remove(key);
        if (removed != null)
        {
            onRemove(level, key, removed);
            if (map.isEmpty()) entriesByLevel.remove(level);
        }
    }

    public void cleanup(ServerLevel level, long ttlTicks)
    {
        var map = entriesByLevel.get(level);
        if (map == null) return;

        long now = level.getGameTime();
        var it = map.long2ObjectEntrySet().fastIterator();

        while (it.hasNext())
        {
            var e = it.next();
            long key = e.getLongKey();
            TEntry entry = e.getValue();

            if (isExpired(entry, now, ttlTicks))
            {
                it.remove();
            }
        }
    }

    protected void put(ServerLevel level, BlockPos pos, TEntry entry)
    {
        entries(level).put(pos.asLong(), entry);
    }

    protected Long2ObjectOpenHashMap<TEntry> entries(ServerLevel level)
    {
        return entriesByLevel.computeIfAbsent(level, l -> new Long2ObjectOpenHashMap<>());
    }

    protected abstract boolean isExpired(TEntry entry, long now, long ttlTicks);

    protected void onRemove(ServerLevel level, long posKey, TEntry entry) {}
}
