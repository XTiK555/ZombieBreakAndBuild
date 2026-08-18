package com.tik.zbb.event;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

public final class MixinEvents
{
    public record OnLevelChunkBlockChangedEvent(ServerLevel level, BlockPos pos, BlockState oldState,
                                                BlockState newState) {}

    public record OnFallingBlockStartedEvent(ServerLevel level, BlockPos startPos, BlockState blockState) {}

    public record OnFallingBlockFinishedEvent(ServerLevel level, BlockPos startPos, BlockPos finalPos,
                                              BlockState blockState, BlockState oldState, CompoundTag oldNbt) {}
}
