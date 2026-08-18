package com.tik.zbb.blockstorage.storages.buildDisappear;

import com.tik.zbb.Constants;
import com.tik.zbb.blockstorage.PersistentExpiringBlockStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public class BuildDisappearBlockStorage extends PersistentExpiringBlockStorage<BuildDisappearBlockStorageEntry>
{
    public static final class OnRemovedEvent
    {
        private final ServerLevel level;
        private final BlockPos pos;
        private final BuildDisappearBlockStorageEntry entry;
        private RemovalResult result = RemovalResult.CONTINUE_REMOVE;

        private OnRemovedEvent(ServerLevel level, BlockPos pos, BuildDisappearBlockStorageEntry entry)
        {
            this.level = level;
            this.pos = pos;
            this.entry = entry;
        }

        public ServerLevel level() {return level;}

        public BlockPos pos() {return pos;}

        public BuildDisappearBlockStorageEntry entry() {return entry;}

        public void cancelAndReassign() {result = RemovalResult.CANCEL_AND_REASSIGN;}
    }

    public BuildDisappearBlockStorage()
    {
        super("build_disappear", BuildDisappearBlockStorageEntry.CODEC);
    }

    @Override
    protected RemovalResult onRemove(ServerLevel level, long posKey, BuildDisappearBlockStorageEntry entry)
    {
        OnRemovedEvent event = new OnRemovedEvent(level, BlockPos.of(posKey), entry);
        Constants.EVENT_BUS.post(event);
        return event.result;
    }
}
