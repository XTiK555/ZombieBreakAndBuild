package com.tik.zbb.blockstorage.storages.damage;

import org.greenrobot.eventbus.Subscribe;

public class DamageBlockVisual
{
    @Subscribe
    public void onDamageEntryAdded(DamageBlockStorageManager.OnEntryAdded event)
    {
        int stage = (int) Math.min(9L, ((long) event.entry().totalDamage() * 10L) / event.blockHealth());

        event.level().destroyBlockProgress(event.entry().blockPosId(), event.pos(), stage);
    }

    @Subscribe
    public void onDamageEntryRemoved(DamageBlockStorage.OnRemovedEvent event)
    {
        event.level().destroyBlockProgress(event.entry().blockPosId(), event.pos(), -1);
    }
}