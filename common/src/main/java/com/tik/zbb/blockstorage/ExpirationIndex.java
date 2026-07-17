package com.tik.zbb.blockstorage;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

final class ExpirationIndex
{
    private static final Snapshot EMPTY_SNAPSHOT = new Snapshot(new LongArrayList(), new LongArrayList());

    private final Long2ObjectRBTreeMap<LongOpenHashSet> positionsByTick = new Long2ObjectRBTreeMap<>();

    void add(long storedAtTick, long posKey)
    {
        positionsByTick.computeIfAbsent(storedAtTick, _ -> new LongOpenHashSet()).add(posKey);
    }

    void remove(long storedAtTick, long posKey)
    {
        LongOpenHashSet positions = positionsByTick.get(storedAtTick);
        if (positions == null) return;

        positions.remove(posKey);
        if (positions.isEmpty())
        {
            positionsByTick.remove(storedAtTick);
        }
    }

    Snapshot collectDueEntries(long processingCutoff, long expirationCutoff)
    {
        if (positionsByTick.isEmpty() || positionsByTick.firstLongKey() > processingCutoff)
        {
            return EMPTY_SNAPSHOT;
        }

        LongArrayList positions = new LongArrayList();
        LongArrayList storedAtTicks = new LongArrayList();
        LongArrayList expiredTicks = new LongArrayList();

        for (Long2ObjectMap.Entry<LongOpenHashSet> tickEntry : positionsByTick.long2ObjectEntrySet())
        {
            long storedAtTick = tickEntry.getLongKey();
            if (storedAtTick > processingCutoff) break;

            for (long posKey : tickEntry.getValue())
            {
                positions.add(posKey);
                storedAtTicks.add(storedAtTick);
            }

            if (storedAtTick <= expirationCutoff)
            {
                expiredTicks.add(storedAtTick);
            }
        }

        for (long storedAtTick : expiredTicks)
        {
            positionsByTick.remove(storedAtTick);
        }

        return new Snapshot(positions, storedAtTicks);
    }

    boolean isEmpty()
    {
        return positionsByTick.isEmpty();
    }

    static final class Snapshot
    {
        private final LongArrayList positions;
        private final LongArrayList storedAtTicks;

        private Snapshot(LongArrayList positions, LongArrayList storedAtTicks)
        {
            this.positions = positions;
            this.storedAtTicks = storedAtTicks;
        }

        int size()
        {
            return positions.size();
        }

        long posKeyAt(int index)
        {
            return positions.getLong(index);
        }

        long storedAtTickAt(int index)
        {
            return storedAtTicks.getLong(index);
        }
    }
}
