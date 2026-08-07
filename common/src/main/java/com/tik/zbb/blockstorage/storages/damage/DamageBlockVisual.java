package com.tik.zbb.blockstorage.storages.damage;

import org.greenrobot.eventbus.Subscribe;

public class DamageBlockVisual
{
    @Subscribe
    public void onDamageBlockDataRemoved(DamageBlockStorage.OnRemovedEvent event)
    {
        event.level().destroyBlockProgress(event.entry().blockPosId(), event.pos(), -1);
    }
}