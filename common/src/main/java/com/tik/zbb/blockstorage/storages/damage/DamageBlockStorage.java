package com.tik.zbb.blockstorage.storages.damage;

import com.tik.zbb.Constants;
import com.tik.zbb.blockstorage.ExpiringBlockStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public class DamageBlockStorage extends ExpiringBlockStorage<DamageBlockStorageEntry>
{
    public record OnRemovedEvent(ServerLevel level, BlockPos pos, DamageBlockStorageEntry entry) {}

    @Override
    protected void onRemove(ServerLevel level, long posKey, DamageBlockStorageEntry entry)
    {
        BlockPos pos = BlockPos.of(posKey);
        Constants.EVENT_BUS.post(new OnRemovedEvent(level, pos, entry));
    }
}
