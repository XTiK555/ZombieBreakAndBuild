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
        if (!context.aiTimers().buildCooldownPassed(context.level().getGameTime())) return false;
        if (!context.level().isLoaded(request.pos())) return false;
        if (context.configSnapshot().game().ai().ignoreBuildEntityIdMatcher()
                .matches(context.mobId(), context.mob().getType().getCategory())) return false;

        return context.level().getBlockState(request.pos()).canBeReplaced();
    }

    @Override
    public boolean execute(MobActionContext context, BuildRequest request)
    {
        BlockState oldState = context.level().getBlockState(request.pos());
        BlockState placedState = request.bridgeBlock().defaultBlockState();
        BlockEntity oldBlockEntity = context.level().getBlockEntity(request.pos());
        CompoundTag oldNbt = oldBlockEntity != null ? oldBlockEntity.saveWithFullMetadata(context.level().registryAccess()) : null;

        boolean placed = context.level().setBlockAndUpdate(request.pos(), placedState);
        if (placed)
        {
            Constants.EVENT_BUS.post(new OnAnyBlockPlacedEvent(context.level(), request.pos(), context.configSnapshot(), placedState, oldState, oldNbt));
        }

        context.aiTimers().setBuildCooldownUntil(context.level().getGameTime() + SecondsToTicksUtility.toTicks(context.configSnapshot().game().balance().cooldowns().buildCooldown(), 1));
        return placed;
    }
}
