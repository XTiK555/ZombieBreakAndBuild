package com.tik.zbb.blockstorage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.tik.zbb.Constants;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public abstract class PersistentExpiringBlockStorage<TData> extends ExpiringBlockStorage<TData>
{
    private final SavedData.Factory<StorageSavedData<TData>> savedDataFactory;
    private final String savedDataName;
    private final Map<ServerLevel, StorageSavedData<TData>> savedDataByLevel = new WeakHashMap<>();

    protected PersistentExpiringBlockStorage(String name, Codec<TData> dataCodec)
    {
        Codec<List<PersistedEntry<TData>>> savedDataCodec = entryCodec(dataCodec).listOf()
                .optionalFieldOf("entries", List.of())
                .codec();

        savedDataFactory = new SavedData.Factory<>(
                () -> new StorageSavedData<>(List.of(), savedDataCodec),
                (tag, registries) -> new StorageSavedData<>(savedDataCodec.parse(
                        registries.createSerializationContext(NbtOps.INSTANCE), tag).getOrThrow(), savedDataCodec),
                null
        );
        savedDataName = Constants.MOD_ID + "_blockstorage_" + name;
    }

    @Override
    protected final void beforeAccess(ServerLevel level)
    {
        if (savedDataByLevel.containsKey(level)) return;

        StorageSavedData<TData> savedData = level.getDataStorage().computeIfAbsent(savedDataFactory, savedDataName);
        savedDataByLevel.put(level, savedData);

        for (PersistedEntry<TData> entry : savedData.entries())
        {
            restoreEntry(level, entry.posKey(), entry.data(), entry.storedAtTick());
        }
    }

    @Override
    protected final void onEntryChanged(ServerLevel level, long posKey, @Nullable StoredEntry<TData> stored)
    {
        StorageSavedData<TData> savedData = savedDataByLevel.get(level);
        if (savedData == null) return;

        if (stored == null)
        {
            savedData.remove(posKey);
        }
        else
        {
            savedData.put(new PersistedEntry<>(posKey, stored.storedAtTick(), stored.data()));
        }
    }

    static <T> Codec<PersistedEntry<T>> entryCodec(Codec<T> dataCodec)
    {
        return RecordCodecBuilder.create(instance -> instance.group(
                Codec.LONG.fieldOf("pos").forGetter(PersistedEntry::posKey),
                Codec.LONG.fieldOf("stored_at").forGetter(PersistedEntry::storedAtTick),
                dataCodec.fieldOf("data").forGetter(PersistedEntry::data)
        ).apply(instance, PersistedEntry::new));
    }

    record PersistedEntry<T>(long posKey, long storedAtTick, T data) {}

    private static final class StorageSavedData<T> extends SavedData
    {
        private final Long2ObjectOpenHashMap<PersistedEntry<T>> entries = new Long2ObjectOpenHashMap<>();
        private final Codec<List<PersistedEntry<T>>> codec;

        private StorageSavedData(List<PersistedEntry<T>> entries, Codec<List<PersistedEntry<T>>> codec)
        {
            this.codec = codec;
            for (PersistedEntry<T> entry : entries)
            {
                this.entries.put(entry.posKey(), entry);
            }
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries)
        {
            CompoundTag encoded = (CompoundTag) codec.encodeStart(
                    registries.createSerializationContext(NbtOps.INSTANCE), entries()).getOrThrow();
            return tag.merge(encoded);
        }

        private List<PersistedEntry<T>> entries()
        {
            return new ArrayList<>(entries.values());
        }

        private void put(PersistedEntry<T> entry)
        {
            if (!entry.equals(entries.put(entry.posKey(), entry))) setDirty();
        }

        private void remove(long posKey)
        {
            if (entries.remove(posKey) != null) setDirty();
        }
    }
}
