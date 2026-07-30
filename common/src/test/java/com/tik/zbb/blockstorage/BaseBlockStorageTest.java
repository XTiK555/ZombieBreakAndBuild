package com.tik.zbb.blockstorage;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BaseBlockStorageTest
{
    private static final BlockPos POS = new BlockPos(1, 2, 3);

    @Test
    void putStoresAndRemoveNotifies()
    {
        TestStorage storage = new TestStorage();

        storage.put(null, POS, "value");
        storage.remove(null, POS);

        assertNull(storage.get(null, POS));
        assertEquals(1, storage.removed);
        assertEquals(1, storage.discarded);
        assertEquals("value", storage.removedValue);
    }

    @Test
    void replacementUsesReplacedHookWithoutRemoveHook()
    {
        TestStorage storage = new TestStorage();
        storage.put(null, POS, "old");

        storage.put(null, POS, "new");

        assertEquals("new", storage.get(null, POS));
        assertEquals(0, storage.removed);
        assertEquals(1, storage.replaced);
        assertEquals(1, storage.discarded);
        assertEquals("old", storage.replacedPrevious);
        assertEquals("new", storage.replacedWith);
    }

    @Test
    void discardRemovesWithoutNotification()
    {
        TestStorage storage = new TestStorage();
        storage.put(null, POS, "value");

        assertEquals("value", storage.discard(null, POS));
        assertFalse(storage.contains(null, POS));
        assertEquals(0, storage.removed);
        assertEquals(1, storage.discarded);
    }

    @Test
    void missingRemoveAndDiscardAreNoOps()
    {
        TestStorage storage = new TestStorage();

        storage.remove(null, POS);

        assertNull(storage.discard(null, POS));
        assertEquals(0, storage.removed);
        assertEquals(0, storage.replaced);
    }

    private static final class TestStorage extends BaseBlockStorage<String, String>
    {
        private int removed;
        private int replaced;
        private int discarded;
        private String removedValue;
        private String replacedPrevious;
        private String replacedWith;

        @Override
        protected String toStored(ServerLevel level, String data)
        {
            return data;
        }

        @Override
        protected String toData(String stored)
        {
            return stored;
        }

        @Override
        protected void onRemove(ServerLevel level, long posKey, String data)
        {
            removed++;
            removedValue = data;
        }

        @Override
        protected void onDiscarded(ServerLevel level, long posKey, String stored)
        {
            discarded++;
        }

        @Override
        protected void onReplaced(ServerLevel level, long posKey, String previous, String replacement)
        {
            replaced++;
            replacedPrevious = previous;
            replacedWith = replacement;
        }
    }
}
