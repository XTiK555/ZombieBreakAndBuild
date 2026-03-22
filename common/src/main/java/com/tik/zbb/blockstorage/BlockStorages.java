package com.tik.zbb.blockstorage;

import com.tik.zbb.blockstorage.storages.broken.BrokenReappearBlockStorageManager;
import com.tik.zbb.blockstorage.storages.buildDisappear.BuildDisappearBlockStorageManager;
import com.tik.zbb.blockstorage.storages.buildProtection.BuildProtectionBlockStorageManager;
import com.tik.zbb.blockstorage.storages.damage.DamageBlockStorageManager;
import com.tik.zbb.blockstorage.storages.id.IdBlockStorageManager;

public final class BlockStorages
{
    public final static DamageBlockStorageManager DAMAGE_MANAGER = new DamageBlockStorageManager();
    public final static BuildProtectionBlockStorageManager BUILD_PROTECTION_MANAGER = new BuildProtectionBlockStorageManager();
    public final static BuildDisappearBlockStorageManager BUILD_DISAPPEAR_MANAGER = new BuildDisappearBlockStorageManager();
    public final static BrokenReappearBlockStorageManager BROKEN_MANAGER = new BrokenReappearBlockStorageManager();
    public final static IdBlockStorageManager ID_MANAGER = new IdBlockStorageManager();
}
