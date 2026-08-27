package com.tik.zbb.blockstorage.storages.damage;

import com.tik.zbb.Constants;
import com.tik.zbb.blockstorage.BlockStorages;
import com.tik.zbb.blockstorage.ExpiringBlockStorage;
import com.tik.zbb.config.ConfigManager;
import com.tik.zbb.event.MixinEvents;
import com.tik.zbb.utilities.BlockHealthCalculator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.greenrobot.eventbus.Subscribe;

public class DamageBlockStorageManager
{
    public record OnEntryAdded(ServerLevel level, BlockPos pos, BlockState state, int blockHealth, DamageBlockStorageEntry entry) {}

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
        Constants.EVENT_BUS.post(new OnEntryAdded(level, pos, level.getBlockState(pos), BlockHealthCalculator.getBlockHealth(pos, level, ConfigManager.getConfigSnapshot()), newEntry));

        return newEntry;
    }

    public void putTimedTotalDamage(ServerLevel level, BlockPos pos, ExpiringBlockStorage.TimedEntry<Integer> damageTimedIntegerEntry)
    {
        int blockId = BlockStorages.ID_MANAGER.getOrCreate(level, pos);
        var timedStorageEntry = new ExpiringBlockStorage.TimedEntry<>(new DamageBlockStorageEntry(damageTimedIntegerEntry.data(), blockId), damageTimedIntegerEntry.storedAtTick());

        damageBlockStorage.putTimed(level, pos, timedStorageEntry);
        Constants.EVENT_BUS.post(new OnEntryAdded(level, pos, level.getBlockState(pos), BlockHealthCalculator.getBlockHealth(pos, level, ConfigManager.getConfigSnapshot()), timedStorageEntry.data()));
    }

    public int getTotalBlockDamage(ServerLevel level, BlockPos pos)
    {
        DamageBlockStorageEntry entry = damageBlockStorage.get(level, pos);
        return entry == null ? 0 : entry.totalDamage();
    }

    public ExpiringBlockStorage.TimedEntry<Integer> getTimedTotalDamage(ServerLevel level, BlockPos pos)
    {
        ExpiringBlockStorage.TimedEntry<DamageBlockStorageEntry> entry = damageBlockStorage.getTimed(level, pos);
        return entry == null ? null : new ExpiringBlockStorage.TimedEntry<>(
                entry.data().totalDamage(),
                entry.storedAtTick()
        );
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
