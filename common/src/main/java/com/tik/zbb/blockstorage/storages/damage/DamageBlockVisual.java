package com.tik.zbb.blockstorage.storages.damage;

import org.greenrobot.eventbus.Subscribe;

public class DamageBlockVisual
{
    @Subscribe
    public void onDamageBlockDataRemoved(DamageBlockStorageManager.OnDamageBlockDataRemovedEvent event)
    {
        event.level().destroyBlockProgress(event.blockPosId(), event.pos(), -1);
    }
}