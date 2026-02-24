package com.tik.zbb.ai.action.actions;

import com.tik.zbb.BlockStorage;
import com.tik.zbb.ai.action.IMobAction;
import com.tik.zbb.ai.action.MobActionContext;
import com.tik.zbb.utilities.SecondsToTicksUtility;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public record BreakAction(BlockPos breakPos, SoundEvent breakSound, SoundEvent hitSound) implements IMobAction
{
    @Override
    public boolean canExecute(MobActionContext context)
    {
        boolean cooldownPassed = context.actionTimers().breakCooldownPassed(context.level().getGameTime());
        boolean notRecentlyBuilt = !BlockStorage.buildMapContains((ServerLevel) context.level(), breakPos);

        return cooldownPassed && notRecentlyBuilt;
    }

    @Override
    public void execute(MobActionContext context)
    {
        BlockState state = context.level().getBlockState(breakPos);
        int blockHealth = getBlockHealth(breakPos, context.level());
        int damageGave = BlockStorage.addDamage(context.level(), breakPos, context.configSnapshot().data().damageToBlocks);

        if (state.isAir()) return;
        if (blockHealth == Integer.MAX_VALUE) return;

        if (damageGave >= blockHealth)
        {
            BlockStorage.removeDamageData((ServerLevel) context.level(), breakPos);
            context.level().destroyBlock(breakPos, true);
            context.level().playSound(null, breakPos, breakSound, SoundSource.HOSTILE, 0.25f, 1.0f);
        }
        else
        {
            context.level().levelEvent(2001, breakPos, Block.getId(state)); // particles
            context.level().playSound(null, breakPos, hitSound, SoundSource.HOSTILE, 0.25f, 1.0f);
        }
        context.executor().executeFreezeAction();
        context.actionTimers().setBreakCooldownUntil(context.level().getGameTime() + SecondsToTicksUtility.toTicks(context.configSnapshot().data().breakCooldown, 1));
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
