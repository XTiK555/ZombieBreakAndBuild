package com.tik.zbb.blockstorage;

import com.tik.zbb.Constants;
import com.tik.zbb.blockstorage.storages.broken.BrokenBlockStorage;
import com.tik.zbb.blockstorage.storages.buildDisappear.BuildDisappearBlockStorage;
import com.tik.zbb.blockstorage.storages.buildProtection.BuildProtectionBlockStorage;
import com.tik.zbb.blockstorage.storages.damage.DamageBlockStorage;

public final class BlockStorages
{
    public final static DamageBlockStorage DAMAGE = new DamageBlockStorage();
    public final static BuildProtectionBlockStorage BUILD_PROTECTION = new BuildProtectionBlockStorage();
    public final static BuildDisappearBlockStorage BUILD_DISAPPEAR = new BuildDisappearBlockStorage();
    public final static BrokenBlockStorage BROKEN = new BrokenBlockStorage();

    public static void init()
    {
        Constants.EVENT_BUS.register(BlockStorages.DAMAGE);
        Constants.EVENT_BUS.register(BlockStorages.BUILD_PROTECTION);
        Constants.EVENT_BUS.register(BlockStorages.BUILD_DISAPPEAR);
        Constants.EVENT_BUS.register(BlockStorages.BROKEN);
    }
}
