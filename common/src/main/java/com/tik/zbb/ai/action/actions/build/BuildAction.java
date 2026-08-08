package com.tik.zbb.ai.action.actions.build;

import com.tik.zbb.Constants;
import com.tik.zbb.ai.action.IMobAction;
import com.tik.zbb.ai.action.MobActionContext;
import com.tik.zbb.config.ConfigSnapshot;
import com.tik.zbb.utilities.SecondsToTicksUtility;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BuildAction implements IMobAction<BuildRequest>
{
    public record OnAnyBlockPlacedEvent(ServerLevel level, BlockPos pos, ConfigSnapshot configSnapshot,
                                        BlockState placedState, BlockState oldState, CompoundTag oldNbt) {}

    @Override
    public boolean canExecute(MobActionContext context, BuildRequest request)
    {
        if (!context.level().isLoaded(request.pos())) return false;

        BlockState blockState = context.level().getBlockState(request.pos());

        boolean canReplaced = blockState.canBeReplaced();
        boolean cooldownPassed = context.aiTimers().buildCooldownPassed(context.level().getGameTime());
        boolean canMobBuild = !context.configSnapshot().game().ai().ignoreBuildEntityIdMatcher()
                .matches(context.mobId(), context.mob().getType().getCategory());

        return cooldownPassed && canReplaced && canMobBuild;
    }

    @Override
    public void execute(MobActionContext context, BuildRequest request)
    {
        BlockState oldState = context.level().getBlockState(request.pos());
        BlockState placedState = request.bridgeBlock().defaultBlockState();
        BlockEntity oldBlockEntity = context.level().getBlockEntity(request.pos());
        CompoundTag oldNbt = oldBlockEntity != null ? oldBlockEntity.saveWithFullMetadata(context.level().registryAccess()) : null;

        if (context.level().setBlockAndUpdate(request.pos(), placedState))
        {
            Constants.EVENT_BUS.post(new OnAnyBlockPlacedEvent(context.level(), request.pos(), context.configSnapshot(), placedState, oldState, oldNbt));
        }

        context.aiTimers().setBuildCooldownUntil(context.level().getGameTime() + SecondsToTicksUtility.toTicks(context.configSnapshot().game().balance().cooldowns().buildCooldown(), 1));
    }
}
