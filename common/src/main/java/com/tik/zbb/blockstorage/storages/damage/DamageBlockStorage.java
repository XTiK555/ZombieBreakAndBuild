package com.tik.zbb.blockstorage.storages.damage;

import com.tik.zbb.Constants;
import com.tik.zbb.blockstorage.BaseBlockStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public class DamageBlockStorage extends BaseBlockStorage<DamageBlockStorageEntry>
{
    public record OnRemovedEvent(ServerLevel level, BlockPos pos, DamageBlockStorageEntry entry) {}

    @Override
    protected boolean isExpired(ServerLevel level, long posKey, DamageBlockStorageEntry entry, long now, long ttlTicks)
    {
        return now - entry.lastTick() >= ttlTicks;
    }

    @Override
    protected void onRemove(ServerLevel level, long posKey, DamageBlockStorageEntry entry)
    {
        BlockPos pos = BlockPos.of(posKey);
        Constants.EVENT_BUS.post(new OnRemovedEvent(level, pos, entry));
    }
}
