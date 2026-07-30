package com.tik.zbb.blockstorage.storages.buildDisappear;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.nbt.CompoundTag;

public record BuildDisappearBlockStorageEntry(BlockState placedState, BlockState oldState, CompoundTag oldNbt) {}
