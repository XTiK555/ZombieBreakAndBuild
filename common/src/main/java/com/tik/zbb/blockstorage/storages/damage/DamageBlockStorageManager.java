package com.tik.zbb.blockstorage.storages.damage;

import com.tik.zbb.ai.action.actions.breakk.BreakAction;
import com.tik.zbb.event.MixinEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.greenrobot.eventbus.Subscribe;

public class DamageBlockStorageManager
{
    private final DamageBlockStorage damageBlockStorage = new DamageBlockStorage();

    @Subscribe
    public void onAnyBlockBroken(BreakAction.OnAnyBlockBrokenEvent event)
    {
        damageBlockStorage.remove(event.level(), event.pos());
    }

    @Subscribe
    public void onLevelChunkBlockChanged(MixinEvents.OnLevelChunkBlockChangedEvent event)
    {
        damageBlockStorage.remove(event.level(), event.pos());
    }

    @Subscribe
    public void onAnyBlockHit(BreakAction.OnAnyBlockHit event)
    {
        DamageBlockStorageEntry current = damageBlockStorage.get(event.level(), event.pos());
        int newDamage = (current == null ? 0 : current.damage()) + event.newDamage();

        damageBlockStorage.put(event.level(), event.pos(), new DamageBlockStorageEntry(newDamage, event.blockId(), event.level().getGameTime()));
    }

    @Subscribe
    public void onDamageBlockStorageRemove(DamageBlockStorage.OnRemovedEvent event)
    {
        event.level().destroyBlockProgress(event.entry().blockPosId(), event.pos(), -1);
    }

    public int getTotalBlockDamage(ServerLevel level, BlockPos pos)
    {
        DamageBlockStorageEntry entry = damageBlockStorage.get(level, pos);
        return entry == null ? 0 : entry.damage();
    }

    public boolean contains(ServerLevel level, BlockPos pos)
    {
        return damageBlockStorage.contains(level, pos);
    }

    public void cleanup(ServerLevel level, long ttlTicks)
    {
        damageBlockStorage.cleanup(level, ttlTicks);
    }
}
