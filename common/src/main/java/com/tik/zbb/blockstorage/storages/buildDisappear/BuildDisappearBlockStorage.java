package com.tik.zbb.blockstorage.storages.buildDisappear;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.tik.zbb.Constants;
import com.tik.zbb.blockstorage.PersistentExpiringBlockStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public class BuildDisappearBlockStorage extends PersistentExpiringBlockStorage<BuildDisappearBlockStorageEntry>
{
    private static final Codec<BuildDisappearBlockStorageEntry> ENTRY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockState.CODEC.fieldOf("placed_state").forGetter(BuildDisappearBlockStorageEntry::placedState),
            BlockState.CODEC.fieldOf("old_state").forGetter(BuildDisappearBlockStorageEntry::oldState),
            CompoundTag.CODEC.optionalFieldOf("old_nbt").forGetter(entry -> Optional.ofNullable(entry.oldNbt()))
    ).apply(instance, (placedState, oldState, oldNbt) -> new BuildDisappearBlockStorageEntry(
            placedState,
            oldState,
            oldNbt.orElse(null)
    )));

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

        public ServerLevel level() { return level; }
        public BlockPos pos() { return pos; }
        public BuildDisappearBlockStorageEntry entry() { return entry; }
        public void cancelAndReassign() { result = RemovalResult.CANCEL_AND_REASSIGN; }
    }

    public BuildDisappearBlockStorage()
    {
        super("build_disappear", ENTRY_CODEC);
    }

    @Override
    protected RemovalResult onRemove(ServerLevel level, long posKey, BuildDisappearBlockStorageEntry entry)
    {
        OnRemovedEvent event = new OnRemovedEvent(level, BlockPos.of(posKey), entry);
        Constants.EVENT_BUS.post(event);
        return event.result;
    }
}
