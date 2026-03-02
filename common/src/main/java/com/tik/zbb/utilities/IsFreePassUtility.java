package com.tik.zbb.utilities;

import com.tik.zbb.config.ConfigData;
import com.tik.zbb.config.ConfigManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class IsFreePassUtility
{
    public static boolean isFreePass(BlockPos pos, ServerLevel level)
    {
        Registry<Block> blockRegistry = level.registryAccess().lookupOrThrow(Registries.BLOCK);

        BlockState blockState = level.getBlockState(pos);
        Identifier id = blockRegistry.getKey(blockState.getBlock());

        if (blockState.isAir()) return true;
        if (id != null && ConfigManager.getConfigSnapshot().data().impassableBlockIdSet.contains(id)) return false;

        return blockState.getCollisionShape(level, pos).isEmpty();
    }
}
