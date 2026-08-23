package com.tik.zbb.blockstorage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.tik.zbb.Constants;
import com.tik.zbb.blockstorage.storages.buildDisappear.BuildDisappearBlockStorageEntry;
import com.tik.zbb.blockstorage.storages.buildDisappear.BuildDisappearBlockStorageManager;
import com.tik.zbb.blockstorage.storages.damage.DamageBlockStorageManager;
import com.tik.zbb.event.MixinEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.greenrobot.eventbus.Subscribe;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class FallingBlockStorageTracker
{
    private static final Codec<ExpiringBlockStorage.TimedEntry<BuildDisappearBlockStorageEntry>> BUILD_DISAPPEAR_CODEC =
            timedEntryCodec(BuildDisappearBlockStorageEntry.CODEC);
    private static final Codec<ExpiringBlockStorage.TimedEntry<Integer>> DAMAGE_CODEC = timedEntryCodec(Codec.INT);

    private final BuildDisappearBlockStorageManager buildDisappearManager = BlockStorages.BUILD_DISAPPEAR_MANAGER;
    private final DamageBlockStorageManager damageManager = BlockStorages.DAMAGE_MANAGER;
    private final SavedDataType<TrackerSavedData> savedDataType;
    private final Map<ServerLevel, TrackerSavedData> savedDataByLevel = new WeakHashMap<>();

    public FallingBlockStorageTracker()
    {
        Codec<TrackerSavedData> codec = PersistedEntry.CODEC.listOf()
                .optionalFieldOf("entries", List.of())
                .xmap(TrackerSavedData::new, TrackerSavedData::entries)
                .codec();

        savedDataType = new SavedDataType<>(
                Identifier.fromNamespaceAndPath(Constants.MOD_ID, "blockstorage/falling_tracker"),
                () -> new TrackerSavedData(List.of()),
                codec,
                null
        );
    }

    @Subscribe
    public void onFallingBlockStarted(MixinEvents.OnFallingBlockStartedEvent event)
    {
        ExpiringBlockStorage.TimedEntry<BuildDisappearBlockStorageEntry> buildDisappear =
                buildDisappearManager.getTimed(event.level(), event.startPos());
        if (buildDisappear != null && !buildDisappear.data().placedState().is(event.blockState().getBlock()))
        {
            buildDisappear = null;
        }

        FallingBlockEntries entries = new FallingBlockEntries(
                buildDisappear,
                damageManager.getTimedTotalDamage(event.level(), event.startPos())
        );

        if (!entries.isEmpty()) savedData(event.level()).put(event.startPos(), entries);
    }

    @Subscribe
    public void onFallingBlockFinished(MixinEvents.OnFallingBlockFinishedEvent event)
    {
        FallingBlockEntries entries = savedData(event.level()).remove(event.startPos());
        if (entries == null || !landedAsExpectedBlock(event)) return;

        restoreBuildDisappearEntry(event, entries.buildDisappear());

        if (entries.damage() != null)
        {
            damageManager.putTimedTotalDamage(event.level(), event.finalPos(), entries.damage());
        }
    }

    private boolean landedAsExpectedBlock(MixinEvents.OnFallingBlockFinishedEvent event)
    {
        return event.level().getBlockState(event.finalPos()).is(event.blockState().getBlock());
    }

    private void restoreBuildDisappearEntry(
            MixinEvents.OnFallingBlockFinishedEvent event,
            @Nullable ExpiringBlockStorage.TimedEntry<BuildDisappearBlockStorageEntry> timedEntry)
    {
        if (timedEntry == null || event.oldState() == null) return;

        BuildDisappearBlockStorageEntry original = timedEntry.data();
        buildDisappearManager.putTimed(event.level(), event.finalPos(), new ExpiringBlockStorage.TimedEntry<>(
                new BuildDisappearBlockStorageEntry(original.placedState(), event.oldState(), event.oldNbt()),
                timedEntry.storedAtTick()
        ));
    }

    private TrackerSavedData savedData(ServerLevel level)
    {
        return savedDataByLevel.computeIfAbsent(
                level,
                ignored -> level.getDataStorage().computeIfAbsent(savedDataType)
        );
    }

    private static <T> Codec<ExpiringBlockStorage.TimedEntry<T>> timedEntryCodec(Codec<T> dataCodec)
    {
        return RecordCodecBuilder.create(instance -> instance.group(
                dataCodec.fieldOf("data").forGetter(ExpiringBlockStorage.TimedEntry::data),
                Codec.LONG.fieldOf("stored_at").forGetter(ExpiringBlockStorage.TimedEntry::storedAtTick)
        ).apply(instance, ExpiringBlockStorage.TimedEntry::new));
    }

    private record FallingBlockEntries(
            @Nullable ExpiringBlockStorage.TimedEntry<BuildDisappearBlockStorageEntry> buildDisappear,
            @Nullable ExpiringBlockStorage.TimedEntry<Integer> damage)
    {
        private boolean isEmpty()
        {
            return buildDisappear == null && damage == null;
        }
    }

    private record PersistedEntry(BlockPos startPos, FallingBlockEntries entries)
    {
        private static final Codec<PersistedEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockPos.CODEC.fieldOf("start_pos").forGetter(PersistedEntry::startPos),
                BUILD_DISAPPEAR_CODEC.optionalFieldOf("build_disappear")
                        .forGetter(entry -> Optional.ofNullable(entry.entries().buildDisappear())),
                DAMAGE_CODEC.optionalFieldOf("damage")
                        .forGetter(entry -> Optional.ofNullable(entry.entries().damage()))
        ).apply(instance, (startPos, buildDisappear, damage) -> new PersistedEntry(
                startPos,
                new FallingBlockEntries(buildDisappear.orElse(null), damage.orElse(null))
        )));
    }

    private static final class TrackerSavedData extends SavedData
    {
        private final Map<BlockPos, FallingBlockEntries> entries = new HashMap<>();

        private TrackerSavedData(List<PersistedEntry> entries)
        {
            for (PersistedEntry entry : entries)
            {
                this.entries.put(entry.startPos(), entry.entries());
            }
        }

        private List<PersistedEntry> entries()
        {
            List<PersistedEntry> persisted = new ArrayList<>(entries.size());
            entries.forEach((startPos, entry) -> persisted.add(new PersistedEntry(startPos, entry)));
            return persisted;
        }

        private void put(BlockPos startPos, FallingBlockEntries entry)
        {
            if (!Objects.equals(entry, entries.put(startPos.immutable(), entry))) setDirty();
        }

        @Nullable
        private FallingBlockEntries remove(BlockPos startPos)
        {
            FallingBlockEntries removed = entries.remove(startPos);
            if (removed != null) setDirty();
            return removed;
        }
    }
}
