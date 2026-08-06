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

    public static final class OnRemovedEvent
    {
        private final ServerLevel level;
        private final BlockPos pos;
        private final BrokenReappearBlockStorageEntry entry;
        private RemovalResult result = RemovalResult.CONTINUE_REMOVE;

        private OnRemovedEvent(ServerLevel level, BlockPos pos, BrokenReappearBlockStorageEntry entry)
        {
            this.level = level;
            this.pos = pos;
            this.entry = entry;
        }

        public ServerLevel level() { return level; }
        public BlockPos pos() { return pos; }
        public BrokenReappearBlockStorageEntry entry() { return entry; }
        public void cancelAndReassign() { result = RemovalResult.CANCEL_AND_REASSIGN; }
    }

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
    protected RemovalResult onRemove(ServerLevel level, long posKey, BrokenReappearBlockStorageEntry entry)
    {
        OnRemovedEvent event = new OnRemovedEvent(level, BlockPos.of(posKey), entry);
        Constants.EVENT_BUS.post(event);
        return event.result;
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
