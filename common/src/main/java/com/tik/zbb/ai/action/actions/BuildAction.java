package com.tik.zbb.ai.action.actions;

import com.tik.zbb.BlockStorage;
import com.tik.zbb.ai.action.IMobAction;
import com.tik.zbb.ai.action.MobActionContext;
import com.tik.zbb.utilities.SecondsToTicksUtility;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class BuildAction implements IMobAction
{
    private BlockPos buildPos;
    private Block bridgeBlock;
    private SoundEvent placeSound;

    @Override
    public boolean canExecute(MobActionContext context)
    {
        BlockState blockState = context.level().getBlockState(buildPos);

        boolean canReplaced = context.level().isLoaded(buildPos) && blockState.canBeReplaced();
        boolean isAir = context.level().isLoaded(buildPos) && blockState.isAir();
        boolean cooldownPassed = context.aiTimers().buildCooldownPassed(context.level().getGameTime());

        return cooldownPassed && (canReplaced || isAir);
    }

    @Override
    public void execute(MobActionContext context)
    {
        context.level().setBlockAndUpdate(buildPos, bridgeBlock.defaultBlockState());
        context.level().playSound(null, buildPos, placeSound, SoundSource.BLOCKS, 0.5f, 1.0f);
        context.executor().tryExecuteFreezeAction();
        context.aiTimers().setBuildCooldownUntil(context.level().getGameTime() + SecondsToTicksUtility.toTicks(context.configSnapshot().data().buildCooldown, 1));
        BlockStorage.addBuild(context.level(), buildPos.immutable());
    }

    public void setup(BlockPos buildPos, Block bridgeBlock, SoundEvent placeSound)
    {
        this.buildPos = buildPos;
        this.bridgeBlock = bridgeBlock;
        this.placeSound = placeSound;
    }
}
