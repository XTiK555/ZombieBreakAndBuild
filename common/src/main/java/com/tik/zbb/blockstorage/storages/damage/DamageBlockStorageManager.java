package com.tik.zbb.blockstorage.storages.damage;

import com.tik.zbb.blockstorage.BlockStorages;
import com.tik.zbb.event.MixinEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.greenrobot.eventbus.Subscribe;

public class DamageBlockStorageManager
{
    private final DamageBlockStorage damageBlockStorage = new DamageBlockStorage();

    @Subscribe
    public void onLevelChunkBlockChanged(MixinEvents.OnLevelChunkBlockChangedEvent event)
    {
        damageBlockStorage.remove(event.level(), event.pos());
    }

    @Subscribe
    public void onDamageBlockStorageRemove(DamageBlockStorage.OnRemovedEvent event)
    {
        BlockStorages.ID_MANAGER.release(event.level(), event.pos(), event.entry().blockPosId());
    }

    public DamageBlockStorageEntry addDamageRecord(ServerLevel level, BlockPos pos, int damage)
    {
        int blockId = BlockStorages.ID_MANAGER.getOrCreate(level, pos);
        DamageBlockStorageEntry newEntry = new DamageBlockStorageEntry(damage, blockId);

        damageBlockStorage.put(level, pos, newEntry);

        return newEntry;
    }

    public int getTotalBlockDamage(ServerLevel level, BlockPos pos)
    {
        DamageBlockStorageEntry entry = damageBlockStorage.get(level, pos);
        return entry == null ? 0 : entry.totalDamage();
    }

    public void addDamageRecord(ServerLevel level, BlockPos pos, int damage, int blockId)
    {
        damageBlockStorage.put(level, pos, new DamageBlockStorageEntry(damage, blockId));
    }

    public void removeRecord(ServerLevel level, BlockPos pos)
    {
        damageBlockStorage.remove(level, pos);
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
