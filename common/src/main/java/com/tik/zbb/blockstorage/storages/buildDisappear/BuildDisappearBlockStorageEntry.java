package com.tik.zbb.blockstorage.storages.buildDisappear;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public record BuildDisappearBlockStorageEntry(BlockState placedState, BlockState oldState, CompoundTag oldNbt)
{
    static final Codec<BuildDisappearBlockStorageEntry> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    BlockState.CODEC
                            .fieldOf("placed_state")
                            .forGetter(BuildDisappearBlockStorageEntry::placedState),
                    BlockState.CODEC
                            .fieldOf("old_state")
                            .forGetter(BuildDisappearBlockStorageEntry::oldState),
                    CompoundTag.CODEC.
                            optionalFieldOf("old_nbt")
                            .forGetter(entry -> Optional.ofNullable(entry.oldNbt()))
            ).apply(instance, (placedState, oldState, oldNbt) ->
                    new BuildDisappearBlockStorageEntry(
                            placedState,
                            oldState,
                            oldNbt.orElse(null)
                    )));
}
