package com.tik.zbb.blockstorage;

import com.tik.zbb.blockstorage.storages.broken.BrokenBlockStorage;
import com.tik.zbb.blockstorage.storages.buildDisappear.BuildDisappearBlockStorage;
import com.tik.zbb.blockstorage.storages.buildProtection.BuildProtectionBlockStorage;
import com.tik.zbb.blockstorage.storages.damage.DamageBlockStorage;
import com.tik.zbb.event.Events;

public final class BlockStorages
{
    public final static DamageBlockStorage DAMAGE = new DamageBlockStorage();
    public final static BuildProtectionBlockStorage BUILD_PROTECTION = new BuildProtectionBlockStorage();
    public final static BuildDisappearBlockStorage BUILD_DISAPPEAR = new BuildDisappearBlockStorage();
    public final static BrokenBlockStorage BROKEN = new BrokenBlockStorage();

    public static void init()
    {
        Events.BUS.register(BlockStorages.DAMAGE);
        Events.BUS.register(BlockStorages.BUILD_PROTECTION);
        Events.BUS.register(BlockStorages.BUILD_DISAPPEAR);
        Events.BUS.register(BlockStorages.BROKEN);
    }
}
