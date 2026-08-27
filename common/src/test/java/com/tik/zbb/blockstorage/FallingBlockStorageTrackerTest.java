package com.tik.zbb.blockstorage;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FallingBlockStorageTrackerTest
{
    @Test
    void fallingBlocksFromSamePositionKeepSeparateEntries()
    {
        FallingBlockStorageTracker.TrackerSavedData data =
                new FallingBlockStorageTracker.TrackerSavedData(List.of());
        UUID firstEntity = UUID.randomUUID();
        UUID secondEntity = UUID.randomUUID();
        BlockPos sharedStartPos = new BlockPos(1, 2, 3);
        FallingBlockStorageTracker.FallingBlockEntries firstEntries = entriesWithDamage(10);
        FallingBlockStorageTracker.FallingBlockEntries secondEntries = entriesWithDamage(20);

        data.put(firstEntity, firstEntries);
        data.put(secondEntity, secondEntries);

        assertEquals(firstEntries, data.remove(firstEntity, sharedStartPos));
        assertEquals(secondEntries, data.remove(secondEntity, sharedStartPos));
    }

    private static FallingBlockStorageTracker.FallingBlockEntries entriesWithDamage(int damage)
    {
        return new FallingBlockStorageTracker.FallingBlockEntries(
                null,
                new ExpiringBlockStorage.TimedEntry<>(damage, 0)
        );
    }
}
