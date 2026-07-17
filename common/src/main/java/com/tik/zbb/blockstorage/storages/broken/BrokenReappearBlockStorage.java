package com.tik.zbb.blockstorage.storages.broken;

import com.tik.zbb.Constants;
import com.tik.zbb.blockstorage.ExpiringBlockStorage;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.Map;
import java.util.WeakHashMap;

public class BrokenReappearBlockStorage extends ExpiringBlockStorage<BrokenReappearBlockStorageEntry>
{
    public record OnWillRemoveEvent(ServerLevel level, BlockPos pos, BrokenReappearBlockStorageEntry entry) {}

    public record OnRemovedEvent(ServerLevel level, BlockPos pos, BrokenReappearBlockStorageEntry entry) {}

    private final Map<ServerLevel, LongOpenHashSet> warnedByLevel = new WeakHashMap<>();
    private static final int WILL_BE_REMOVED_OFFSET = 16;

    @Override
    protected long earlyProcessingTicks()
    {
        return WILL_BE_REMOVED_OFFSET;
    }

    @Override
    protected void onExpiringSoon(ServerLevel level, long posKey, BrokenReappearBlockStorageEntry entry)
    {
        if (warned(level).add(posKey))
        {
            Constants.EVENT_BUS.post(new OnWillRemoveEvent(level, BlockPos.of(posKey), entry));
        }
    }

    @Override
    protected void onEntryStored(ServerLevel level, long posKey)
    {
        clearWarned(level, posKey);
    }

    @Override
    protected void onEntryDiscarded(ServerLevel level, long posKey)
    {
        clearWarned(level, posKey);
    }

    @Override
    protected void onRemove(ServerLevel level, long posKey, BrokenReappearBlockStorageEntry entry)
    {
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
