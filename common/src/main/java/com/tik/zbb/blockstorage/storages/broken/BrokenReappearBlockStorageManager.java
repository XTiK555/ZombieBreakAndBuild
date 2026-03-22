package com.tik.zbb.blockstorage.storages.broken;

import com.tik.zbb.Constants;
import com.tik.zbb.ai.action.actions.breakk.BreakAction;
import com.tik.zbb.blockstorage.BlockStorages;
import com.tik.zbb.config.ConfigSnapshot;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Clearable;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.greenrobot.eventbus.Subscribe;

import java.util.Map;
import java.util.WeakHashMap;

public class BrokenReappearBlockStorageManager
{
    public record OnBrokenBlockReappearEvent(ServerLevel level, BlockPos pos, BlockState newState) {}

    public record OnBrokenBlockWillReappearEvent(ServerLevel level, BlockPos pos, BlockState newState) {}

    private final BrokenReappearBlockStorage brokenReappearBlockStorage = new BrokenReappearBlockStorage();
    private final Map<ServerLevel, Long2ObjectOpenHashMap<BrokenReappearBlockStorageEntry>> pendingEntriesByLevel = new WeakHashMap<>();

    @Subscribe
    public void onAnyBlockWillBroke(BreakAction.OnAnyBlockWillBrokeEvent event)
    {
        if (!brokenBlockStorageAddConditions(event.configSnapshot(), event.level(), event.pos())) return;

        BlockEntity blockEntity = event.level().getBlockEntity(event.pos());
        CompoundTag blockEntityTag = null;
        if (blockEntity != null)
        {
            blockEntityTag = blockEntity.saveWithFullMetadata(event.level().registryAccess());
        }

        BrokenReappearBlockStorageEntry newPending = new BrokenReappearBlockStorageEntry(event.state(), blockEntityTag, event.level().getGameTime());
        pendingEntries(event.level()).put(event.pos().asLong(), newPending);

        if (blockEntity instanceof Clearable clearable)
        {
            clearable.clearContent();
            blockEntity.setChanged();
        }
    }

    @Subscribe
    public void onAnyBlockFailedToBroke(BreakAction.OnAnyBlockFailedToBrokeEvent event)
    {
        if (!brokenBlockStorageAddConditions(event.configSnapshot(), event.level(), event.pos())) return;

        BrokenReappearBlockStorageEntry entry = getPending(event.level(), event.pos());
        if (entry == null) return;

        BlockEntity blockEntity = event.level().getBlockEntity(event.pos());
        BlockState currentState = event.level().getBlockState(event.pos());

        if (blockEntity == null || entry.nbt() == null || currentState.getBlock() != entry.oldState().getBlock())
        {
            removePending(event.level(), event.pos());
            return;
        }

        restoreBlockEntity(event.level(), event.pos(), entry.nbt());
        removePending(event.level(), event.pos());
    }

    @Subscribe
    public void onAnyBlockBroken(BreakAction.OnAnyBlockBrokenEvent event)
    {
        if (!brokenBlockStorageAddConditions(event.configSnapshot(), event.level(), event.pos())) return;

        BrokenReappearBlockStorageEntry pendingEntry = getPending(event.level(), event.pos());
        if (pendingEntry == null) return;

        removePending(event.level(), event.pos());
        brokenReappearBlockStorage.put(event.level(), event.pos(), new BrokenReappearBlockStorageEntry(pendingEntry.oldState(), pendingEntry.nbt(), event.level().getGameTime()));
    }

    @Subscribe
    public void onBrokenBlockStorageRemove(BrokenReappearBlockStorage.OnRemovedEvent event)
    {
        restoreBlock(event.level(), event.pos(), event.entry());
        Constants.EVENT_BUS.post(new OnBrokenBlockReappearEvent(event.level(), event.pos(), event.entry().oldState()));
    }

    @Subscribe
    public void onBrokenBlockStorageWillRemove(BrokenReappearBlockStorage.OnWillRemoveEvent event)
    {
        Constants.EVENT_BUS.post(new OnBrokenBlockWillReappearEvent(event.level(), event.pos(), event.entry().oldState()));
    }

    public boolean contains(ServerLevel level, BlockPos pos)
    {
        return brokenReappearBlockStorage.contains(level, pos);
    }

    public void cleanup(ServerLevel level, long ttlTicks)
    {
        brokenReappearBlockStorage.cleanup(level, ttlTicks);
    }

    private void restoreBlock(ServerLevel level, BlockPos pos, BrokenReappearBlockStorageEntry entry)
    {
        BlockState currentState = level.getBlockState(pos);

        if (!currentState.isAir())
        {
            level.destroyBlock(pos, true);
        }

        level.setBlockAndUpdate(pos, entry.oldState());
        restoreBlockEntity(level, pos, entry.nbt());
    }

    private void restoreBlockEntity(ServerLevel level, BlockPos pos, CompoundTag savedTag)
    {
        if (savedTag == null) return;

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) return;

        CompoundTag tagToLoad = savedTag.copy();
        tagToLoad.putInt("x", pos.getX());
        tagToLoad.putInt("y", pos.getY());
        tagToLoad.putInt("z", pos.getZ());

        blockEntity.loadWithComponents(tagToLoad, level.registryAccess());
        blockEntity.setChanged();

        BlockState state = level.getBlockState(pos);
        level.sendBlockUpdated(pos, state, state, 3);
    }

    private void removePending(ServerLevel level, BlockPos pos)
    {
        var map = pendingEntriesByLevel.get(level);
        if (map == null) return;

        map.remove(pos.asLong());
        if (map.isEmpty())
        {
            pendingEntriesByLevel.remove(level);
        }
    }

    private BrokenReappearBlockStorageEntry getPending(ServerLevel level, BlockPos pos)
    {
        var map = pendingEntriesByLevel.get(level);
        return map != null ? map.get(pos.asLong()) : null;
    }

    private Long2ObjectOpenHashMap<BrokenReappearBlockStorageEntry> pendingEntries(ServerLevel level)
    {
        return pendingEntriesByLevel.computeIfAbsent(level, l -> new Long2ObjectOpenHashMap<>());
    }

    private boolean brokenBlockStorageAddConditions(ConfigSnapshot configSnapshot, ServerLevel level, BlockPos pos)
    {
        if (!configSnapshot.data().blockRestoration.brokenBlocksRestoring) return false;
        if (BlockStorages.BUILD_DISAPPEAR_MANAGER.contains(level, pos)) return false;

        return true;
    }
}
