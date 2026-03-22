package com.tik.zbb.blockstorage.storages.broken;

import com.tik.zbb.Constants;
import com.tik.zbb.blockstorage.BaseBlockStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public class BrokenReappearBlockStorage extends BaseBlockStorage<BrokenReappearBlockStorageEntry>
{
    private final int WILL_BE_REMOVED_OFFSET = 16;

    public record OnWillRemoveEvent(ServerLevel level, BlockPos pos, BrokenReappearBlockStorageEntry entry) {}

    public record OnRemovedEvent(ServerLevel level, BlockPos pos, BrokenReappearBlockStorageEntry entry) {}

    @Override
    protected boolean isExpired(ServerLevel level, long posKey, BrokenReappearBlockStorageEntry entry, long now, long ttlTicks)
    {
        long timePassed = now - entry.tick();
        long ticksLeft = ttlTicks - timePassed;

        if (ticksLeft == WILL_BE_REMOVED_OFFSET)
        {
            Constants.EVENT_BUS.post(new OnWillRemoveEvent(level, BlockPos.of(posKey), entry));
        }

        return timePassed >= ttlTicks;
    }

    @Override
    protected void onRemove(ServerLevel level, long posKey, BrokenReappearBlockStorageEntry entry)
    {
        BlockPos pos = BlockPos.of(posKey);
        Constants.EVENT_BUS.post(new OnRemovedEvent(level, pos, entry));
    }
}