package com.tik.zbb.blockstorage.storages.broken;

import com.tik.zbb.ai.action.actions.breakk.BreakAction;
import com.tik.zbb.blockstorage.BaseBlockStorage;
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

public class BrokenBlockStorage extends BaseBlockStorage<BrokenBlockStorageEntry>
{
    private final Map<ServerLevel, Long2ObjectOpenHashMap<BrokenBlockStorageEntry>> pendingEntriesByLevel = new WeakHashMap<>();

    @Subscribe
    public void onAnyBlockWillBroke(BreakAction.OnAnyBlockWillBrokeEvent event)
    {
        if (!BrokenBlockStorageAddConditions(event.configSnapshot(), event.level(), event.pos())) return;

        BlockEntity blockEntity = event.level().getBlockEntity(event.pos());
        CompoundTag blockEntityTag = null;
        if (blockEntity != null)
        {
            blockEntityTag = blockEntity.saveWithFullMetadata(event.level().registryAccess());
        }

        BrokenBlockStorageEntry newPending = new BrokenBlockStorageEntry(event.state(), blockEntityTag, event.level().getGameTime());
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
        if (!BrokenBlockStorageAddConditions(event.configSnapshot(), event.level(), event.pos())) return;

        BrokenBlockStorageEntry entry = getPending(event.level(), event.pos());
        if (entry == null) return;

        BlockEntity blockEntity = event.level().getBlockEntity(event.pos());
        BlockState currentState = event.level().getBlockState(event.pos());

        if (blockEntity == null || entry.blockEntityTag() == null || currentState.getBlock() != entry.oldState().getBlock())
        {
            removePending(event.level(), event.pos());
            return;
        }

        restoreBlockEntity(event.level(), event.pos(), entry.blockEntityTag());
        removePending(event.level(), event.pos());
    }

    @Subscribe
    public void onAnyBlockBroken(BreakAction.OnAnyBlockBrokenEvent event)
    {
        if (!BrokenBlockStorageAddConditions(event.configSnapshot(), event.level(), event.pos())) return;

        BrokenBlockStorageEntry pendingEntry = getPending(event.level(), event.pos());
        if (pendingEntry == null) return;

        removePending(event.level(), event.pos());
        put(event.level(), event.pos(), pendingEntry);
    }

    public void addBrokenData(ServerLevel level, BlockPos pos, BlockState oldState)
    {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        CompoundTag blockEntityTag = null;
        if (blockEntity != null)
        {
            blockEntityTag = blockEntity.saveWithFullMetadata(level.registryAccess());
        }

        put(level, pos, new BrokenBlockStorageEntry(oldState, blockEntityTag, level.getGameTime()));
    }

    @Override
    protected boolean isExpired(BrokenBlockStorageEntry entry, long now, long ttlTicks)
    {
        return now - entry.tick() > ttlTicks;
    }

    @Override
    protected void onRemove(ServerLevel level, long posKey, BrokenBlockStorageEntry entry)
    {
        restoreBlock(level, BlockPos.of(posKey), entry);
    }

    private void restoreBlock(ServerLevel level, BlockPos pos, BrokenBlockStorageEntry entry)
    {
        BlockState currentState = level.getBlockState(pos);

        if (!currentState.isAir())
        {
            level.destroyBlock(pos, true);
        }

        level.setBlockAndUpdate(pos, entry.oldState());
        restoreBlockEntity(level, pos, entry.blockEntityTag());
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

    private BrokenBlockStorageEntry getPending(ServerLevel level, BlockPos pos)
    {
        var map = pendingEntriesByLevel.get(level);
        return map != null ? map.get(pos.asLong()) : null;
    }

    private Long2ObjectOpenHashMap<BrokenBlockStorageEntry> pendingEntries(ServerLevel level)
    {
        return pendingEntriesByLevel.computeIfAbsent(level, l -> new Long2ObjectOpenHashMap<>());
    }

    private boolean BrokenBlockStorageAddConditions(ConfigSnapshot configSnapshot, ServerLevel level, BlockPos pos)
    {
        if (!configSnapshot.data().blockRestoration.brokenBlocksRestoring) return false;
        if (BlockStorages.BUILD_DISAPPEAR.contains(level, pos)) return false;

        return true;
    }
}
