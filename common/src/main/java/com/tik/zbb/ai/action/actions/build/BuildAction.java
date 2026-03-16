package com.tik.zbb.ai.action.actions.build;

import com.tik.zbb.Constants;
import com.tik.zbb.ai.action.IMobAction;
import com.tik.zbb.ai.action.MobActionContext;
import com.tik.zbb.config.ConfigSnapshot;
import com.tik.zbb.utilities.SecondsToTicksUtility;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

public class BuildAction implements IMobAction<BuildRequest>
{
    @Override
    public boolean canExecute(MobActionContext context, BuildRequest request)
    {
        BlockState blockState = context.level().getBlockState(request.pos());

        boolean canReplaced = context.level().isLoaded(request.pos()) && blockState.canBeReplaced();
        boolean cooldownPassed = context.aiTimers().buildCooldownPassed(context.level().getGameTime());
        boolean canMobBuild = !context.configSnapshot().data().ignoreBuildEntityIdSet.contains(context.mobId());

        return cooldownPassed && canReplaced && canMobBuild;
    }

    @Override
    public void execute(MobActionContext context, BuildRequest request)
    {
        BlockState placedState = request.bridgeBlock().defaultBlockState();
        SoundType soundType = placedState.getSoundType();

        context.level().setBlockAndUpdate(request.pos(), request.bridgeBlock().defaultBlockState());
        context.level().playSound(null, request.pos(), soundType.getPlaceSound(), SoundSource.BLOCKS, soundType.volume * context.configSnapshot().data().audio.placeSoundVolumeMultiplier, soundType.pitch);

        context.executor().tryExecuteFreezeAction();
        context.aiTimers().setBuildCooldownUntil(context.level().getGameTime() + SecondsToTicksUtility.toTicks(context.configSnapshot().data().balance.cooldowns.buildCooldown, 1));
        Constants.EVENT_BUS.post(new OnAnyBlockPlacedEvent(context.level(), request.pos(), context.configSnapshot()));
    }

    public record OnAnyBlockPlacedEvent(ServerLevel level, BlockPos pos, ConfigSnapshot configSnapshot) {}
}
