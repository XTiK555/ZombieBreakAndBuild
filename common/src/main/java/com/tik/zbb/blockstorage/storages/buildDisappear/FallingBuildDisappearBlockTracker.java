package com.tik.zbb.blockstorage.storages.buildDisappear;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.tik.zbb.Constants;
import com.tik.zbb.blockstorage.BlockStorages;
import com.tik.zbb.event.MixinEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.greenrobot.eventbus.Subscribe;

import java.util.*;

public final class FallingBuildDisappearBlockTracker
{
    private final BuildDisappearBlockStorageManager storage;
    private final SavedDataType<TrackerSavedData> savedDataType;
    private final Map<ServerLevel, TrackerSavedData> savedDataByLevel = new WeakHashMap<>();

    public FallingBuildDisappearBlockTracker()
    {
        this.storage = BlockStorages.BUILD_DISAPPEAR_MANAGER;

        Codec<TrackerSavedData> codec = PersistedEntry.CODEC.listOf()
                .optionalFieldOf("entries", List.of())
                .xmap(TrackerSavedData::new, TrackerSavedData::entries)
                .codec();

        savedDataType = new SavedDataType<>(
                Identifier.fromNamespaceAndPath(Constants.MOD_ID, "blockstorage/falling_build_disappear"),
                () -> new TrackerSavedData(List.of()),
                codec,
                null
        );
    }

    @Subscribe
    public void onFallingBlockStarted(MixinEvents.OnFallingBlockStartedEvent event)
    {
        BuildDisappearBlockStorageEntry entry = storage.get(event.level(), event.startPos());

        if (entry == null || !entry.placedState().is(event.blockState().getBlock())) return;

        savedData(event.level()).put(event.startPos(), entry);
    }

    @Subscribe
    public void onFallingBlockFinished(MixinEvents.OnFallingBlockFinishedEvent event)
    {
        BuildDisappearBlockStorageEntry entry = savedData(event.level()).remove(event.startPos());

        if (entry == null || event.oldState() == null) return;

        if (event.level().getBlockState(event.finalPos()).is(event.blockState().getBlock()))
        {
            storage.put(event.level(), event.finalPos(), new BuildDisappearBlockStorageEntry(
                    entry.placedState(),
                    event.oldState(),
                    event.oldNbt()
            ));
        }
    }

    private TrackerSavedData savedData(ServerLevel level)
    {
        return savedDataByLevel.computeIfAbsent(
                level,
                ignored -> level.getDataStorage().computeIfAbsent(savedDataType)
        );
    }

    private record PersistedEntry(BlockPos startPos, BuildDisappearBlockStorageEntry entry)
    {
        private static final Codec<PersistedEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockPos.CODEC.fieldOf("start_pos").forGetter(PersistedEntry::startPos),
                BuildDisappearBlockStorageEntry.CODEC.fieldOf("entry").forGetter(PersistedEntry::entry)
        ).apply(instance, PersistedEntry::new));
    }

    private static final class TrackerSavedData extends SavedData
    {
        private final Map<BlockPos, BuildDisappearBlockStorageEntry> entries = new HashMap<>();

        private TrackerSavedData(List<PersistedEntry> entries)
        {
            for (PersistedEntry entry : entries)
            {
                this.entries.put(entry.startPos(), entry.entry());
            }
        }

        private List<PersistedEntry> entries()
        {
            List<PersistedEntry> persisted = new ArrayList<>(entries.size());
            entries.forEach((startPos, entry) -> persisted.add(new PersistedEntry(startPos, entry)));
            return persisted;
        }

        private void put(BlockPos startPos, BuildDisappearBlockStorageEntry entry)
        {
            if (!entry.equals(entries.put(startPos.immutable(), entry))) setDirty();
        }

        private BuildDisappearBlockStorageEntry remove(BlockPos startPos)
        {
            BuildDisappearBlockStorageEntry removed = entries.remove(startPos);
            if (removed != null) setDirty();
            return removed;
        }
    }
}
