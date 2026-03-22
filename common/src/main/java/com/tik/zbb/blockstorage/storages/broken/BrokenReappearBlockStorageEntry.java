package com.tik.zbb.blockstorage.storages.broken;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

public record BrokenReappearBlockStorageEntry(BlockState oldState, CompoundTag nbt, long tick) {}
