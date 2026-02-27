package com.tik.zbb.utilities;

import com.tik.zbb.config.ConfigData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class IsFreePassUtility
{
    public static boolean isFreePass(BlockPos pos, Registry<Block> blockRegistry, ServerLevel level, ConfigData configData)
    {
        BlockState blockState = level.getBlockState(pos);
        Identifier id = blockRegistry.getKey(blockState.getBlock());

        if (blockState.isAir()) return true;
        if (id != null && configData.impassableBlockIdSet.contains(id)) return false;

        return blockState.getCollisionShape(level, pos).isEmpty();
    }
}
