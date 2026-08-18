package com.tik.zbb.blockstorage;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.WeakHashMap;

public abstract class BaseBlockStorage<TData, TStored>
{
    public enum RemovalResult
    {
        CONTINUE_REMOVE,
        CANCEL_AND_REASSIGN
    }

    private final Map<ServerLevel, Long2ObjectOpenHashMap<TStored>> entriesByLevel = new WeakHashMap<>();

    @Nullable
    public TData get(ServerLevel level, BlockPos pos)
    {
        beforeAccess(level);
        TStored stored = getStored(level, pos.asLong());
        return stored != null ? toData(stored) : null;
    }

    public boolean contains(ServerLevel level, BlockPos pos)
    {
        beforeAccess(level);
        var entries = entriesByLevel.get(level);
        return entries != null && entries.containsKey(pos.asLong());
    }

    public void put(ServerLevel level, BlockPos pos, TData data)
    {
        beforeAccess(level);
        long posKey = pos.asLong();
        TStored stored = toStored(level, data);
        TStored previous = getStored(level, posKey);

        if (previous != null)
        {
            removeStored(level, posKey, previous, false);
        }

        entries(level).put(posKey, stored);

        onStored(level, posKey, previous, stored);

        if (previous != null)
        {
            onReplaced(level, posKey, toData(previous), data);
        }
    }

    public TData remove(ServerLevel level, BlockPos pos)
    {
        return remove(level, pos.asLong());
    }

    public TData remove(ServerLevel level, long posKey)
    {
        beforeAccess(level);
        TStored removed = removeStored(level, posKey, null, true);
        return removed != null ? toData(removed) : null;
    }

    @Nullable
    public TData discard(ServerLevel level, BlockPos pos)
    {
        beforeAccess(level);
        TStored removed = removeStored(level, pos.asLong(), null, false);
        return removed != null ? toData(removed) : null;
    }

    @Nullable
    protected final TStored getStored(ServerLevel level, long posKey)
    {
        var entries = entriesByLevel.get(level);
        return entries != null ? entries.get(posKey) : null;
    }

    protected final boolean removeStoredIfSame(ServerLevel level, long posKey, TStored expected)
    {
        return removeStored(level, posKey, expected, true) != null;
    }

    protected final void restoreStored(ServerLevel level, long posKey, TStored stored)
    {
        entries(level).put(posKey, stored);
        onStored(level, posKey, null, stored);
    }

    protected void beforeAccess(ServerLevel level) {}

    protected abstract TStored toStored(ServerLevel level, TData data);

    protected abstract TData toData(TStored stored);

    protected void onStored(ServerLevel level, long posKey, @Nullable TStored previous, TStored stored) {}

    protected void onDiscarded(ServerLevel level, long posKey, TStored stored) {}

    protected void onReplaced(ServerLevel level, long posKey, TData previous, TData replacement) {}

    protected RemovalResult onRemove(ServerLevel level, long posKey, TData data)
    {
        return RemovalResult.CONTINUE_REMOVE;
    }

    private Long2ObjectOpenHashMap<TStored> entries(ServerLevel level)
    {
        return entriesByLevel.computeIfAbsent(level, _ -> new Long2ObjectOpenHashMap<>());
    }

    @Nullable
    private TStored removeStored(ServerLevel level, long posKey, @Nullable TStored expected, boolean notify)
    {
        var entries = entriesByLevel.get(level);
        if (entries == null) return null;

        TStored stored = entries.get(posKey);
        if (stored == null || expected != null && stored != expected) return null;

        entries.remove(posKey);
        onDiscarded(level, posKey, stored);

        if (notify)
        {
            TData data = toData(stored);
            if (onRemove(level, posKey, data) == RemovalResult.CANCEL_AND_REASSIGN)
            {
                if (getStored(level, posKey) == null)
                {
                    TStored reassigned = toStored(level, data);
                    entries(level).put(posKey, reassigned);
                    onStored(level, posKey, null, reassigned);
                }
                return null;
            }
        }

        if (entries.isEmpty() && entriesByLevel.get(level) == entries)
        {
            entriesByLevel.remove(level);
        }

        return stored;
    }
}
