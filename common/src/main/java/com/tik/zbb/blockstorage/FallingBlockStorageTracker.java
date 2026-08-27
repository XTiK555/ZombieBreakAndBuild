package com.tik.zbb.blockstorage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.tik.zbb.Constants;
import com.tik.zbb.blockstorage.storages.buildDisappear.BuildDisappearBlockStorageEntry;
import com.tik.zbb.blockstorage.storages.buildDisappear.BuildDisappearBlockStorageManager;
import com.tik.zbb.blockstorage.storages.damage.DamageBlockStorageManager;
import com.tik.zbb.event.MixinEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
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
        BlockPos startPos = event.entity().getStartPos();
        ExpiringBlockStorage.TimedEntry<BuildDisappearBlockStorageEntry> buildDisappear = buildDisappearManager.getTimed(event.level(), startPos);

        if (buildDisappear != null && !buildDisappear.data().placedState().is(event.entity().getBlockState().getBlock()))
        {
            buildDisappear = null;
        }

        FallingBlockEntries entries = new FallingBlockEntries(
                buildDisappear,
                damageManager.getTimedTotalDamage(event.level(), startPos)
        );

        if (!entries.isEmpty()) savedData(event.level()).put(event.entity().getUUID(), entries);
    }

    @Subscribe
    public void onFallingBlockFinished(MixinEvents.OnFallingBlockFinishedEvent event)
    {
        FallingBlockEntries entries = savedData(event.level()).remove(
                event.entity().getUUID(),
                event.entity().getStartPos()
        );

        if (entries == null || !landedAsExpectedBlock(event)) return;

        restoreBuildDisappearEntry(event, entries.buildDisappear());
        restoreDamageEntry(event, entries.damage());
    }

    private void restoreBuildDisappearEntry(MixinEvents.OnFallingBlockFinishedEvent event, @Nullable ExpiringBlockStorage.TimedEntry<BuildDisappearBlockStorageEntry> timedEntry)
    {
        if (timedEntry == null || event.oldState() == null) return;

        BuildDisappearBlockStorageEntry original = timedEntry.data();
        buildDisappearManager.putTimed(event.level(), event.entity().blockPosition(), new ExpiringBlockStorage.TimedEntry<>(
                new BuildDisappearBlockStorageEntry(original.placedState(), event.oldState(), event.oldNbt()),
                timedEntry.storedAtTick()
        ));
    }

    private void restoreDamageEntry(MixinEvents.OnFallingBlockFinishedEvent event, @Nullable ExpiringBlockStorage.TimedEntry<Integer> timedEntry)
    {
        if (timedEntry == null) return;

        damageManager.putTimedTotalDamage(event.level(), event.entity().blockPosition(), timedEntry);
    }

    private boolean landedAsExpectedBlock(MixinEvents.OnFallingBlockFinishedEvent event)
    {
        return event.level().getBlockState(event.entity().blockPosition()).is(event.entity().getBlockState().getBlock());
    }

    // region Data

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

    record FallingBlockEntries(
            @Nullable ExpiringBlockStorage.TimedEntry<BuildDisappearBlockStorageEntry> buildDisappear,
            @Nullable ExpiringBlockStorage.TimedEntry<Integer> damage)
    {
        private boolean isEmpty()
        {
            return buildDisappear == null && damage == null;
        }
    }

    private record PersistedEntry(@Nullable UUID entityId, @Nullable BlockPos legacyStartPos, FallingBlockEntries entries)
    {
        private static final Codec<PersistedEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                UUIDUtil.CODEC.optionalFieldOf("entity_uuid")
                        .forGetter(entry -> Optional.ofNullable(entry.entityId())),
                BlockPos.CODEC.optionalFieldOf("start_pos")
                        .forGetter(entry -> Optional.ofNullable(entry.legacyStartPos())),
                BUILD_DISAPPEAR_CODEC.optionalFieldOf("build_disappear")
                        .forGetter(entry -> Optional.ofNullable(entry.entries().buildDisappear())),
                DAMAGE_CODEC.optionalFieldOf("damage")
                        .forGetter(entry -> Optional.ofNullable(entry.entries().damage()))
        ).apply(instance, (entityId, startPos, buildDisappear, damage) -> new PersistedEntry(
                entityId.orElse(null),
                startPos.orElse(null),
                new FallingBlockEntries(buildDisappear.orElse(null), damage.orElse(null))
        )));
    }

    static final class TrackerSavedData extends SavedData
    {
        private final Map<UUID, FallingBlockEntries> entries = new HashMap<>();
        private final Map<BlockPos, FallingBlockEntries> legacyEntries = new HashMap<>();

        TrackerSavedData(List<PersistedEntry> entries)
        {
            for (PersistedEntry entry : entries)
            {
                if (entry.entityId() != null) this.entries.put(entry.entityId(), entry.entries());
                else if (entry.legacyStartPos() != null) legacyEntries.put(entry.legacyStartPos(), entry.entries());
            }
        }

        private List<PersistedEntry> entries()
        {
            List<PersistedEntry> persisted = new ArrayList<>(entries.size() + legacyEntries.size());
            entries.forEach((entityId, entry) -> persisted.add(new PersistedEntry(entityId, null, entry)));
            legacyEntries.forEach((startPos, entry) -> persisted.add(new PersistedEntry(null, startPos, entry)));
            return persisted;
        }

        void put(UUID entityId, FallingBlockEntries entry)
        {
            if (!Objects.equals(entry, entries.put(entityId, entry))) setDirty();
        }

        @Nullable
        FallingBlockEntries remove(UUID entityId, BlockPos startPos)
        {
            FallingBlockEntries removed = entries.remove(entityId);
            if (removed == null) removed = legacyEntries.remove(startPos);
            if (removed != null) setDirty();
            return removed;
        }
    }

    // endregion
}
