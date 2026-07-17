package com.tik.zbb.blockstorage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpirationIndexTest
{
    @Test
    void collectDueEntriesIncludesTheCutoffAndLeavesFutureBucketsAlone()
    {
        ExpirationIndex index = new ExpirationIndex();
        index.add(10, 100);
        index.add(10, 101);
        index.add(11, 102);

        ExpirationIndex.Snapshot due = index.collectDueEntries(10, 10);

        assertEquals(2, due.size());
        assertEquals(10, due.storedAtTickAt(0));
        assertEquals(10, due.storedAtTickAt(1));

        ExpirationIndex.Snapshot future = index.collectDueEntries(11, 11);
        assertEquals(1, future.size());
        assertEquals(102, future.posKeyAt(0));
        assertTrue(index.isEmpty());
    }

    @Test
    void removingAnOldBucketMembershipPreservesTheReplacement()
    {
        ExpirationIndex index = new ExpirationIndex();
        index.add(10, 100);
        index.remove(10, 100);
        index.add(11, 100);

        assertEquals(0, index.collectDueEntries(10, 10).size());

        ExpirationIndex.Snapshot replacement = index.collectDueEntries(11, 11);
        assertEquals(1, replacement.size());
        assertEquals(100, replacement.posKeyAt(0));
        assertEquals(11, replacement.storedAtTickAt(0));
    }

    @Test
    void warningWindowEntriesRemainIndexedUntilTheirExpirationCutoff()
    {
        ExpirationIndex index = new ExpirationIndex();
        index.add(100, 200);

        ExpirationIndex.Snapshot warningWindow = index.collectDueEntries(100, 84);
        assertEquals(1, warningWindow.size());

        ExpirationIndex.Snapshot repeatedWarningWindow = index.collectDueEntries(100, 85);
        assertEquals(1, repeatedWarningWindow.size());

        ExpirationIndex.Snapshot expired = index.collectDueEntries(100, 100);
        assertEquals(1, expired.size());
        assertTrue(index.isEmpty());
    }
}
