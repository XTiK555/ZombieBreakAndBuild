package com.tik.zbb.blockstorage.storages.buildDisappear;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

public record BuildDisappearEntry(BlockState state, long tick) {}
