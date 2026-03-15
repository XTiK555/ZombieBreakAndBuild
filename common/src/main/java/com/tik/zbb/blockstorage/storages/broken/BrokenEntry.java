package com.tik.zbb.blockstorage.storages.broken;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

public record BrokenEntry(BlockState state, CompoundTag blockEntityTag, long tick) {}
