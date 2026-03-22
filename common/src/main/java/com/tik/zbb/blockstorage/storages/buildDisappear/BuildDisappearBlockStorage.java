package com.tik.zbb.blockstorage.storages.buildDisappear;

import com.tik.zbb.Constants;
import com.tik.zbb.blockstorage.BaseBlockStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public class BuildDisappearBlockStorage extends BaseBlockStorage<BuildDisappearBlockStorageEntry>
{
    public record OnRemovedEvent(ServerLevel level, BlockPos pos, BuildDisappearBlockStorageEntry entry) {}

    @Override
    protected boolean isExpired(ServerLevel level, long posKey, BuildDisappearBlockStorageEntry entry, long now, long ttlTicks)
    {
        return now - entry.tick() >= ttlTicks;
    }

    @Override
    protected void onRemove(ServerLevel level, long posKey, BuildDisappearBlockStorageEntry entry)
    {
        BlockPos pos = BlockPos.of(posKey);
        Constants.EVENT_BUS.post(new OnRemovedEvent(level, pos, entry));
    }
}
