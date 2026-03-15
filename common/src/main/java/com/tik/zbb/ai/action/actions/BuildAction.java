package com.tik.zbb.ai.action.actions;

import com.tik.zbb.ai.action.IMobAction;
import com.tik.zbb.ai.action.MobActionContext;
import com.tik.zbb.blockstorage.BlockStorages;
import com.tik.zbb.utilities.SecondsToTicksUtility;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

public class BuildAction implements IMobAction
{
    private BlockPos buildPos;
    private Block bridgeBlock;

    @Override
    public boolean canExecute(MobActionContext context)
    {
        BlockState blockState = context.level().getBlockState(buildPos);

        boolean canReplaced = context.level().isLoaded(buildPos) && blockState.canBeReplaced();
        boolean cooldownPassed = context.aiTimers().buildCooldownPassed(context.level().getGameTime());
        boolean canMobBuild = !context.configSnapshot().data().ignoreBuildEntityIdSet.contains(context.mobId());

        return cooldownPassed && canReplaced && canMobBuild;
    }

    @Override
    public void execute(MobActionContext context)
    {
        BlockState placedState = bridgeBlock.defaultBlockState();
        SoundType soundType = placedState.getSoundType();

        context.level().setBlockAndUpdate(buildPos, bridgeBlock.defaultBlockState());
        context.level().playSound(null, buildPos, soundType.getPlaceSound(), SoundSource.BLOCKS, soundType.volume * context.configSnapshot().data().audio.placeSoundVolumeMultiplier, soundType.pitch);

        context.executor().tryExecuteFreezeAction();
        context.aiTimers().setBuildCooldownUntil(context.level().getGameTime() + SecondsToTicksUtility.toTicks(context.configSnapshot().data().balance.cooldowns.buildCooldown, 1));
        BlockStorages.BUILD_PROTECTION.addBuildProtectionData(context.level(), buildPos.immutable());
        if (context.configSnapshot().data().blockReturning.builtBlocksDisappearing)
            BlockStorages.BUILD_DISAPPEAR.addBuildDisappearData(context.level(), buildPos.immutable());
    }

    public void setup(BlockPos buildPos, Block bridgeBlock)
    {
        this.buildPos = buildPos;
        this.bridgeBlock = bridgeBlock;
    }
}
