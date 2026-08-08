package com.tik.zbb.blockstorage;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.NbtOps;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PersistentExpiringBlockStorageTest
{
    @Test
    void persistedEntryCodecRoundTripsPositionTimeAndData()
    {
        var codec = PersistentExpiringBlockStorage.entryCodec(Codec.STRING);
        var expected = new PersistentExpiringBlockStorage.PersistedEntry<>(42L, 123L, "data");
        var encoded = codec.encodeStart(NbtOps.INSTANCE, expected).getOrThrow();

        assertEquals(expected, codec.parse(NbtOps.INSTANCE, encoded).getOrThrow());
    }
}
