package com.tik.zbb.blockstorage.storages.broken;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public record BrokenReappearBlockStorageEntry(BlockState oldState, CompoundTag nbt, BlockEntityType<?> blockEntityType)
{
    static final Codec<BrokenReappearBlockStorageEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockState.CODEC
                    .fieldOf("old_state")
                    .forGetter(BrokenReappearBlockStorageEntry::oldState),
            CompoundTag.CODEC
                    .optionalFieldOf("block_entity_nbt")
                    .forGetter(entry -> Optional.ofNullable(entry.nbt())),
            BuiltInRegistries.BLOCK_ENTITY_TYPE.byNameCodec()
                    .optionalFieldOf("block_entity_type")
                    .forGetter(entry -> Optional.ofNullable(entry.blockEntityType()))
    ).apply(instance, (state, nbt, type) ->
            new BrokenReappearBlockStorageEntry(
                    state,
                    nbt.orElse(null),
                    type.orElse(null)
            )));
}
