package com.tik.zbb.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

public final class MixinEvents
{
    public record OnLevelChunkBlockChangedEvent(ServerLevel level, BlockPos pos, BlockState oldState,
                                                BlockState newState) {}
}
