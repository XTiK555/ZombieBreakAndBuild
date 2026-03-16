package com.tik.zbb.blockstorage.storages.broken;

import com.tik.zbb.ai.action.actions.breakk.BreakAction;
import com.tik.zbb.blockstorage.BaseBlockStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Clearable;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import org.greenrobot.eventbus.Subscribe;

public class BrokenBlockStorage extends BaseBlockStorage<BrokenBlockStorageEntry>
{
    @Subscribe
    public void onAnyBlockWillBroke(BreakAction.OnAnyBlockWillBrokeEvent event)
    {
        if (!event.configSnapshot().data().blockReturning.brokenBlocksRestoring) return;

        addBrokenData(event.level(), event.pos(), event.state());

        BlockEntity blockEntity = event.level().getBlockEntity(event.pos());
        if (blockEntity instanceof Clearable clearable)
        {
            clearable.clearContent();
            blockEntity.setChanged();
        }
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
        if (ttlTicks <= 0) return false;
        return now - entry.tick() > ttlTicks;
    }

    @Override
    protected void onRemove(ServerLevel level, long posKey, BrokenBlockStorageEntry entry)
    {
        BlockPos pos = BlockPos.of(posKey);
        BlockState currentState = level.getBlockState(pos);

        if (!currentState.isAir())
        {
            level.destroyBlock(pos, true);
        }
        level.setBlockAndUpdate(pos, entry.oldState());

        CompoundTag savedTag = entry.blockEntityTag();
        BlockEntity restoredBlockEntity = level.getBlockEntity(pos);

        if (savedTag == null || restoredBlockEntity == null) return;

        CompoundTag tagToLoad = savedTag.copy();

        tagToLoad.putInt("x", pos.getX());
        tagToLoad.putInt("y", pos.getY());
        tagToLoad.putInt("z", pos.getZ());

        ValueInput input = TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tagToLoad);

        restoredBlockEntity.loadWithComponents(input);
        restoredBlockEntity.setChanged();

        BlockState restoredState = level.getBlockState(pos);
        level.sendBlockUpdated(pos, restoredState, restoredState, 3);
    }
}
