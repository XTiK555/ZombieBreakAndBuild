package com.tik.zbb.blockstorage;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

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
            if (map.isEmpty() && entriesByLevel.get(level) == map) entriesByLevel.remove(level);
        }
    }

    public void remove(ServerLevel level, long posKey)
    {
        var map = entriesByLevel.get(level);
        if (map == null) return;

        TEntry removed = map.remove(posKey);
        if (removed != null)
        {
            onRemove(level, posKey, removed);
            if (map.isEmpty() && entriesByLevel.get(level) == map) entriesByLevel.remove(level);
        }
    }

    @Nullable
    public TEntry discard(ServerLevel level, BlockPos pos)
    {
        Long2ObjectMap<TEntry> entries = entriesByLevel.get(level);

        if (entries == null)
        {
            return null;
        }

        TEntry removed = entries.remove(pos.asLong());

        if (entries.isEmpty())
        {
            entriesByLevel.remove(level);
        }

        return removed;
    }

    public void cleanup(ServerLevel level, long ttlTicks)
    {
        Long2ObjectMap<TEntry> map = entriesByLevel.get(level);
        if (map == null || map.isEmpty()) return;

        LongArrayList expiredKeys = null;
        long now = level.getGameTime();

        for (Long2ObjectMap.Entry<TEntry> entry : map.long2ObjectEntrySet())
        {
            if (isExpired(level, entry.getLongKey(), entry.getValue(), now, ttlTicks))
            {
                if (expiredKeys == null) expiredKeys = new LongArrayList();
                expiredKeys.add(entry.getLongKey());
            }
        }

        if (expiredKeys == null) return;

        for (long posKey : expiredKeys)
        {
            remove(level, posKey);
        }
    }

    public void put(ServerLevel level, BlockPos pos, TEntry entry)
    {
        entries(level).put(pos.asLong(), entry);
    }

    protected Long2ObjectOpenHashMap<TEntry> entries(ServerLevel level)
    {
        return entriesByLevel.computeIfAbsent(level, _ -> new Long2ObjectOpenHashMap<>());
    }

    protected abstract boolean isExpired(ServerLevel level, long posKey, TEntry entry, long now, long ttlTicks);

    protected void onRemove(ServerLevel level, long posKey, TEntry entry) {}
}
