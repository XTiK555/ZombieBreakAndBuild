package com.tik.zbb.ai.action.actions;

import com.tik.zbb.BlockStorage;
import com.tik.zbb.ai.action.IMobAction;
import com.tik.zbb.ai.action.MobActionContext;
import com.tik.zbb.utilities.SecondsToTicksUtility;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

public class BreakAction implements IMobAction
{
    private BlockPos breakPos;

    @Override
    public boolean canExecute(MobActionContext context)
    {
        boolean isAir = context.level().getBlockState(breakPos).isAir();
        boolean cooldownPassed = context.aiTimers().breakCooldownPassed(context.level().getGameTime());
        boolean notRecentlyBuilt = !BlockStorage.buildMapContains((ServerLevel) context.level(), breakPos);
        boolean unbreakable = getBlockHealth(breakPos, context.level()) == Integer.MAX_VALUE;

        return cooldownPassed && notRecentlyBuilt && !isAir && !unbreakable;
    }

    @Override
    public void execute(MobActionContext context)
    {
        BlockState state = context.level().getBlockState(breakPos);
        int blockHealth = getBlockHealth(breakPos, context.level());
        int damageGave = BlockStorage.addDamage(context.level(), breakPos, context.configSnapshot().data().balance.damageToBlocks);


        if (damageGave >= blockHealth)
        {
            BlockStorage.removeDamageData((ServerLevel) context.level(), breakPos);
            BlockStorage.removeDamageData(context.level(), breakPos);
            context.level().destroyBlock(breakPos, true);
        }
        else
        {
            int stage = Math.min(9, (damageGave * 10) / blockHealth);
            context.level().playSound(null, breakPos, hitSound, SoundSource.HOSTILE, 0.25f, 1.0f);
            context.level().levelEvent(2001, breakPos, Block.getId(state)); // particles and sound
        }

        context.executor().tryExecuteFreezeAction();
        context.aiTimers().setBreakCooldownUntil(context.level().getGameTime() + SecondsToTicksUtility.toTicks(context.configSnapshot().data().balance.breakCooldown, 1));
    }

    public void setup(BlockPos breakPos)
    {
        this.breakPos = breakPos;
    }

    private int getBlockHealth(BlockPos blockPos, ServerLevel level)
    {
        BlockState blockState = level.getBlockState(blockPos);
        float hardness = blockState.getDestroySpeed(level, blockPos);
        if (hardness < 0) return Integer.MAX_VALUE;
        if (hardness != Integer.MAX_VALUE) hardness = Math.min(hardness, 50.0f);
        return hardness != Integer.MAX_VALUE ? Math.max(2, (int) (hardness * 6.0f)) : Integer.MAX_VALUE;
    }
}
