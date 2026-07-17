package com.tik.zbb.blockstorage.storages.buildDisappear;

import com.tik.zbb.Constants;
import com.tik.zbb.blockstorage.ExpiringBlockStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public class BuildDisappearBlockStorage extends ExpiringBlockStorage<BuildDisappearBlockStorageEntry>
{
    public record OnRemovedEvent(ServerLevel level, BlockPos pos, BuildDisappearBlockStorageEntry entry) {}

    @Override
    protected void onRemove(ServerLevel level, long posKey, BuildDisappearBlockStorageEntry entry)
    {
        BlockPos pos = BlockPos.of(posKey);
        Constants.EVENT_BUS.post(new OnRemovedEvent(level, pos, entry));
    }
}
