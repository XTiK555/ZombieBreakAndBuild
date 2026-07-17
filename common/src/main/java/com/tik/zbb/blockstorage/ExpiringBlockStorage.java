package com.tik.zbb.blockstorage;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.WeakHashMap;

public abstract class ExpiringBlockStorage<TData> extends BaseBlockStorage<TData, ExpiringBlockStorage.StoredEntry<TData>>
{
    record StoredEntry<TData>(TData data, long storedAtTick) {}

    private final Map<ServerLevel, ExpirationIndex> expirationIndexesByLevel = new WeakHashMap<>();

    public final void cleanup(ServerLevel level, long ttlTicks)
    {
        ExpirationIndex expirationIndex = expirationIndexesByLevel.get(level);
        if (expirationIndex == null || expirationIndex.isEmpty()) return;

        long now = level.getGameTime();
        long expirationCutoff = now - ttlTicks;
        long processingCutoff = expirationCutoff + earlyProcessingTicks();

        ExpirationIndex.Snapshot dueEntries = expirationIndex.collectDueEntries(processingCutoff, expirationCutoff);
        ObjectArrayList<StoredEntry<TData>> expectedEntries = new ObjectArrayList<>(dueEntries.size());
        
        for (int i = 0; i < dueEntries.size(); i++)
        {
            StoredEntry<TData> stored = getStored(level, dueEntries.posKeyAt(i));
            expectedEntries.add(stored != null && stored.storedAtTick() == dueEntries.storedAtTickAt(i)
                    ? stored
                    : null);
        }
        removeEmptyIndex(level, expirationIndex);

        for (int i = 0; i < dueEntries.size(); i++)
        {
            long posKey = dueEntries.posKeyAt(i);
            StoredEntry<TData> expected = expectedEntries.get(i);
            if (expected == null) continue;

            if (expected.storedAtTick() <= expirationCutoff)
            {
                removeStoredIfSame(level, posKey, expected);
            }
            else if (getStored(level, posKey) == expected)
            {
                onExpiringSoon(level, posKey, expected.data());
            }
        }
    }

    @Override
    protected final StoredEntry<TData> toStored(ServerLevel level, TData data)
    {
        return new StoredEntry<>(data, level.getGameTime());
    }

    @Override
    protected final TData toData(StoredEntry<TData> stored)
    {
        return stored.data();
    }

    @Override
    protected final void onStored(ServerLevel level, long posKey, @Nullable StoredEntry<TData> previous,
                                  StoredEntry<TData> stored)
    {
        if (previous != null)
        {
            removeFromIndex(level, posKey, previous.storedAtTick());
        }

        expirationIndex(level).add(stored.storedAtTick(), posKey);

        onEntryStored(level, posKey);
    }

    @Override
    protected final void onDiscarded(ServerLevel level, long posKey, StoredEntry<TData> stored)
    {
        removeFromIndex(level, posKey, stored.storedAtTick());
        onEntryDiscarded(level, posKey);
    }

    protected long earlyProcessingTicks()
    {
        return 0;
    }

    protected void onExpiringSoon(ServerLevel level, long posKey, TData data) {}

    protected void onEntryStored(ServerLevel level, long posKey) {}

    protected void onEntryDiscarded(ServerLevel level, long posKey) {}

    private ExpirationIndex expirationIndex(ServerLevel level)
    {
        return expirationIndexesByLevel.computeIfAbsent(level, _ -> new ExpirationIndex());
    }

    private void removeFromIndex(ServerLevel level, long posKey, long storedAtTick)
    {
        ExpirationIndex expirationIndex = expirationIndexesByLevel.get(level);
        if (expirationIndex == null) return;

        expirationIndex.remove(storedAtTick, posKey);
        removeEmptyIndex(level, expirationIndex);
    }

    private void removeEmptyIndex(ServerLevel level, ExpirationIndex expirationIndex)
    {
        if (expirationIndex.isEmpty() && expirationIndexesByLevel.get(level) == expirationIndex)
        {
            expirationIndexesByLevel.remove(level);
        }
    }
}
