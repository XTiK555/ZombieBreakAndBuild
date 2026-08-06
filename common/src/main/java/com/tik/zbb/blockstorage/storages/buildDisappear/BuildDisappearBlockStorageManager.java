package com.tik.zbb.blockstorage.storages.buildDisappear;

import com.tik.zbb.Constants;
import com.tik.zbb.ai.action.actions.build.BuildAction;
import com.tik.zbb.event.MixinEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import org.greenrobot.eventbus.Subscribe;

public class BuildDisappearBlockStorageManager
{
    public record OnBuildBlockDisappearEvent(ServerLevel level, BlockPos pos, BlockState placedState) {}

    private final BuildDisappearBlockStorage buildDisappearBlockStorage = new BuildDisappearBlockStorage();

    @Subscribe
    public void onAnyBlockPlaced(BuildAction.OnAnyBlockPlacedEvent event)
    {
        if (!event.configSnapshot().game().blockRestoration().builtBlocksDisappearing()) return;

        buildDisappearBlockStorage.put(event.level(), event.pos(),
                new BuildDisappearBlockStorageEntry(event.placedState(), event.oldState(), event.oldNbt()));
    }

    @Subscribe
    public void onLevelChunkBlockChanged(MixinEvents.OnLevelChunkBlockChangedEvent event)
    {
        buildDisappearBlockStorage.remove(event.level(), event.pos());
    }

    @Subscribe
    public void onBuildDisappearBlockStorageRemove(BuildDisappearBlockStorage.OnRemovedEvent event)
    {
        BlockState currentState = event.level().getBlockState(event.pos());

        if (currentState.is(event.entry().placedState().getBlock()))
        {
            if (!restoreOldBlock(event))
            {
                event.cancelAndReassign();
                return;
            }

            Constants.EVENT_BUS.post(new OnBuildBlockDisappearEvent(event.level(), event.pos(), event.entry().placedState()));
        }
        else if (currentState.is(Blocks.AIR))
        {
            if (!restoreOldBlock(event)) event.cancelAndReassign();
        }
    }

    private boolean restoreOldBlock(BuildDisappearBlockStorage.OnRemovedEvent event)
    {
        if (!event.level().setBlockAndUpdate(event.pos(), event.entry().oldState())) return false;

        CompoundTag savedNbt = event.entry().oldNbt();
        BlockEntity blockEntity = event.level().getBlockEntity(event.pos());
        if (savedNbt == null || blockEntity == null) return true;

        CompoundTag nbt = savedNbt.copy();
        nbt.putInt("x", event.pos().getX());
        nbt.putInt("y", event.pos().getY());
        nbt.putInt("z", event.pos().getZ());
        blockEntity.loadWithComponents(TagValueInput.create(
                ProblemReporter.DISCARDING,
                event.level().registryAccess(),
                nbt
        ));
        blockEntity.setChanged();

        BlockState state = event.level().getBlockState(event.pos());
        event.level().sendBlockUpdated(event.pos(), state, state, 3);
        return true;
    }

    public void discard(ServerLevel level, BlockPos pos)
    {
        buildDisappearBlockStorage.discard(level, pos);
    }

    public BuildDisappearBlockStorageEntry get(ServerLevel level, BlockPos pos)
    {
        return buildDisappearBlockStorage.get(level, pos);
    }

    public boolean contains(ServerLevel level, BlockPos pos)
    {
        return buildDisappearBlockStorage.contains(level, pos);
    }

    public void cleanup(ServerLevel level, long ttlTicks)
    {
        buildDisappearBlockStorage.cleanup(level, ttlTicks);
    }
}
