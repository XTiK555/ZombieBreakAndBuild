package com.tik.zbb.blockstorage.storages.broken;

import com.tik.zbb.Constants;
import com.tik.zbb.blockstorage.BaseBlockStorage;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.Map;
import java.util.WeakHashMap;

public class BrokenReappearBlockStorage extends BaseBlockStorage<BrokenReappearBlockStorageEntry>
{
    public record OnWillRemoveEvent(ServerLevel level, BlockPos pos, BrokenReappearBlockStorageEntry entry) {}

    public record OnRemovedEvent(ServerLevel level, BlockPos pos, BrokenReappearBlockStorageEntry entry) {}

    private final Map<ServerLevel, LongOpenHashSet> warnedByLevel = new WeakHashMap<>();
    private final int WILL_BE_REMOVED_OFFSET = 16;

    @Override
    public void put(ServerLevel level, BlockPos pos, BrokenReappearBlockStorageEntry entry)
    {
        clearWarned(level, pos.asLong());
        super.put(level, pos, entry);
    }

    @Override
    protected boolean isExpired(ServerLevel level, long posKey, BrokenReappearBlockStorageEntry entry, long now, long ttlTicks)
    {
        long timePassed = now - entry.tick();
        long ticksLeft = ttlTicks - timePassed;

        if (ticksLeft > 0 && ticksLeft <= WILL_BE_REMOVED_OFFSET && warned(level).add(posKey))
        {
            Constants.EVENT_BUS.post(new OnWillRemoveEvent(level, BlockPos.of(posKey), entry));
        }

        return timePassed >= ttlTicks;
    }

    @Override
    protected void onRemove(ServerLevel level, long posKey, BrokenReappearBlockStorageEntry entry)
    {
        clearWarned(level, posKey);

        Constants.EVENT_BUS.post(new OnRemovedEvent(level, BlockPos.of(posKey), entry));
    }

    private LongOpenHashSet warned(ServerLevel level)
    {
        return warnedByLevel.computeIfAbsent(
                level,
                ignored -> new LongOpenHashSet()
        );
    }

    private void clearWarned(ServerLevel level, long posKey)
    {
        LongOpenHashSet warned = warnedByLevel.get(level);
        if (warned == null) return;

        warned.remove(posKey);

        if (warned.isEmpty() && warnedByLevel.get(level) == warned)
        {
            warnedByLevel.remove(level);
        }
    }
}