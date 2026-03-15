package com.tik.zbb.ai.action.actions.build;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;

public record BuildRequest(BlockPos pos, Block bridgeBlock) {}
