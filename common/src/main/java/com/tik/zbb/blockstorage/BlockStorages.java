package com.tik.zbb.blockstorage;

import com.tik.zbb.blockstorage.storages.build.BuildBlockStorage;
import com.tik.zbb.blockstorage.storages.damage.DamageBlockStorage;

public final class BlockStorages
{
    public final static DamageBlockStorage DAMAGE = new DamageBlockStorage();
    public final static BuildBlockStorage BUILD = new BuildBlockStorage();
}
