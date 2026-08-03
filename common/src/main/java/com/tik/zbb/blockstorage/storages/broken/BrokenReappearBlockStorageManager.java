package com.tik.zbb.blockstorage.storages.broken;

import com.tik.zbb.Constants;
import com.tik.zbb.ai.action.actions.breakk.BreakAction;
import com.tik.zbb.blockstorage.BlockStorages;
import com.tik.zbb.config.ConfigSnapshot;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import org.greenrobot.eventbus.Subscribe;

import java.util.Map;
import java.util.WeakHashMap;

public class BrokenReappearBlockStorageManager
{
    public record OnBrokenBlockReappearEvent(ServerLevel level, BlockPos pos, BlockState newState) {}

    public record OnBrokenBlockWillReappearEvent(ServerLevel level, BlockPos pos, BlockState newState) {}

    public record OnBrokenBlockStoredEvent(ServerLevel level, BlockPos pos, BrokenReappearBlockStorageEntry entry) {}

    private final BrokenReappearBlockStorage brokenReappearBlockStorage = new BrokenReappearBlockStorage();
    private final Map<ServerLevel, Long2ObjectOpenHashMap<BrokenReappearBlockStorageEntry>> pendingEntriesByLevel = new WeakHashMap<>();

    @Subscribe
    public void onAnyBlockWillBroke(BreakAction.OnAnyBlockWillBrokeEvent event)
    {
        if (!brokenBlockStorageAddConditions(event.configSnapshot(), event.level(), event.pos())) return;

        BlockEntity blockEntity = event.level().getBlockEntity(event.pos());
        BlockEntityType<?> blockEntityType = blockEntity != null ? blockEntity.getType() : null;
        CompoundTag blockEntityTag = null;
        if (blockEntity != null)
            blockEntityTag = blockEntity.saveWithFullMetadata(event.level().registryAccess());

        BrokenReappearBlockStorageEntry newPending = new BrokenReappearBlockStorageEntry(event.state(), blockEntityTag, blockEntityType);
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
        brokenReappearBlockStorage.put(event.level(), event.pos(), pendingEntry);
        Constants.EVENT_BUS.post(new OnBrokenBlockStoredEvent(event.level(), event.pos(), pendingEntry));
    }

    @Subscribe
    public void onBrokenBlockStorageRemove(BrokenReappearBlockStorage.OnRemovedEvent event)
    {
        boolean normalReappear = true;

        if (!restoreBlock(event.level(), event.pos(), event.entry()))
        {
            normalReappear = false;

            if (!giveToNearestPlayer(event.level(), event.pos(), event.entry()))
            {
                placeStoredBlockNearby(event.level(), event.pos(), event.entry(), 2);
            }
        }

        if (normalReappear)
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

    public boolean contains(ServerLevel level, BlockPos pos, BrokenReappearBlockStorageEntry entry)
    {
        return brokenReappearBlockStorage.get(level, pos) == entry;
    }

    public void cleanup(ServerLevel level, long ttlTicks)
    {
        brokenReappearBlockStorage.cleanup(level, ttlTicks);
    }

    private boolean brokenBlockStorageAddConditions(ConfigSnapshot configSnapshot, ServerLevel level, BlockPos pos)
    {
        if (!configSnapshot.game().blockRestoration().brokenBlocksRestoring()) return false;
        if (BlockStorages.ZOMBIE_PLACED_MANAGER.contains(level, pos)) return false;

        return true;
    }

    //region Restoring

    private boolean restoreBlock(ServerLevel level, BlockPos pos, BrokenReappearBlockStorageEntry entry)
    {
        BlockState currentState = level.getBlockState(pos);
        boolean isTrackedZombieBlock = BlockStorages.ZOMBIE_PLACED_MANAGER.contains(level, pos);

        if (currentState.isAir())
        {
            return placeStoredBlock(level, pos, entry);
        }
        if (isTrackedZombieBlock)
        {
            BlockStorages.ZOMBIE_PLACED_MANAGER.remove(level, pos);

            level.destroyBlock(pos, false);

            return placeStoredBlock(level, pos, entry);
        }

        return dropStoredBlock(level, pos, entry);
    }

    private boolean placeStoredBlock(ServerLevel level, BlockPos pos, BrokenReappearBlockStorageEntry entry)
    {
        if (!level.setBlockAndUpdate(pos, entry.oldState()))
        {
            return false;
        }

        restoreBlockEntity(level, pos, entry.nbt());
        return true;
    }

    private boolean dropStoredBlock(ServerLevel level, BlockPos pos, BrokenReappearBlockStorageEntry entry)
    {
        ItemStack stack = createStoredBlockStack(entry);
        if (stack == ItemStack.EMPTY) return false;

        ItemEntity itemEntity = new ItemEntity(
                level,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                stack
        );

        itemEntity.setDefaultPickUpDelay();

        return level.addFreshEntity(itemEntity);
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

        ValueInput input = TagValueInput.create(
                ProblemReporter.DISCARDING,
                level.registryAccess(),
                tagToLoad
        );

        blockEntity.loadWithComponents(input);
        blockEntity.setChanged();

        BlockState state = level.getBlockState(pos);
        level.sendBlockUpdated(pos, state, state, 3);
    }

    private boolean giveToNearestPlayer(ServerLevel level, BlockPos pos, BrokenReappearBlockStorageEntry entry)
    {
        ItemStack stack = createStoredBlockStack(entry);
        if (stack == ItemStack.EMPTY) return false;

        Player player = level.getNearestPlayer(
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                64.0D,
                false
        );

        if (player == null) return false;
        if (player.getInventory().add(stack)) return true;

        return player.drop(stack, false) != null;
    }

    private boolean placeStoredBlockNearby(ServerLevel level, BlockPos originalPos, BrokenReappearBlockStorageEntry entry, int radius)
    {
        BlockState storedState = entry.oldState();

        for (int distance = 1; distance <= radius; distance++)
        {
            for (int yOffset = -distance; yOffset <= distance; yOffset++)
            {
                for (int xOffset = -distance; xOffset <= distance; xOffset++)
                {
                    for (int zOffset = -distance; zOffset <= distance; zOffset++)
                    {
                        int maxDistance = Math.max(Math.abs(xOffset), Math.max(Math.abs(yOffset), Math.abs(zOffset)));

                        if (maxDistance != distance) continue;

                        BlockPos candidatePos = originalPos.offset(
                                xOffset,
                                yOffset,
                                zOffset
                        );

                        if (!level.hasChunkAt(candidatePos)) continue;
                        if (!level.getBlockState(candidatePos).isAir()) continue;
                        if (!storedState.canSurvive(level, candidatePos)) continue;

                        if (placeStoredBlock(level, candidatePos, entry)) return true;
                    }
                }
            }
        }

        return false;
    }

    //endregion

    //region Local

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

    private ItemStack createStoredBlockStack(BrokenReappearBlockStorageEntry entry)
    {
        ItemStack stack = new ItemStack(entry.oldState().getBlock().asItem());
        if (stack.isEmpty()) return ItemStack.EMPTY;

        if (entry.nbt() != null && !entry.nbt().isEmpty())
        {
            if (entry.blockEntityType() == null)
            {
                return ItemStack.EMPTY;
            }

            CompoundTag blockEntityNbt = entry.nbt().copy();
            blockEntityNbt.remove("x");
            blockEntityNbt.remove("y");
            blockEntityNbt.remove("z");

            stack.set(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.of(entry.blockEntityType(), blockEntityNbt));
        }

        return stack;
    }

    //endregion
}
