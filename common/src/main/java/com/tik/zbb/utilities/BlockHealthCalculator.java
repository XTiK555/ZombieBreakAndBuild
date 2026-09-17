package com.tik.zbb.utilities;

import com.tik.zbb.config.ConfigRuntime;
import com.tik.zbb.config.ConfigSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

public class BlockHealthCalculator
{
    public static boolean isUnbreakableBlock(BlockState blockState, BlockPos blockPos, ServerLevel level, ConfigSnapshot configSnapshot)
    {
        return getBlockHealth(blockState, blockPos, level, configSnapshot) == Integer.MAX_VALUE;
    }

    public static int getBlockHealth(BlockState blockState, BlockPos blockPos, ServerLevel level, ConfigSnapshot configSnapshot)
    {
        ConfigRuntime.BlockDamage blockDamageCfg = configSnapshot.game().balance().blockDamage();
        Integer blockHealthOverride = blockDamageCfg.blockHealthOverrideMap().get(blockState.getBlock());
        float hardness = blockState.getDestroySpeed(level, blockPos);
        double health = Math.pow(hardness, blockDamageCfg.blockHardnessExponent()) * blockDamageCfg.blockHardnessMultiplier();

        if (blockHealthOverride != null) return Math.max(1, blockHealthOverride);
        if (exceedsMaximumBreakableHardness(hardness, blockDamageCfg)) return Integer.MAX_VALUE;
        if (hardness < 0) return Integer.MAX_VALUE;
        if (health >= Integer.MAX_VALUE) return Integer.MAX_VALUE;

        return Math.max(1, (int) Math.round(health));
    }

    private static boolean exceedsMaximumBreakableHardness(float hardness, ConfigRuntime.BlockDamage blockDamage)
    {
        return blockDamage.maximumBreakableBlockHardness() > 0.0f && hardness > blockDamage.maximumBreakableBlockHardness();
    }
}
