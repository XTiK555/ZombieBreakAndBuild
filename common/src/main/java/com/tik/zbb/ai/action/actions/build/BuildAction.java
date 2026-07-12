package com.tik.zbb.ai.action.actions.build;

import com.tik.zbb.Constants;
import com.tik.zbb.ai.action.IMobAction;
import com.tik.zbb.ai.action.MobActionContext;
import com.tik.zbb.config.ConfigSnapshot;
import com.tik.zbb.utilities.SecondsToTicksUtility;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

public class BuildAction implements IMobAction<BuildRequest>
{
    public record OnAnyBlockPlacedEvent(ServerLevel level, BlockPos pos, ConfigSnapshot configSnapshot,
                                        BlockState placedState, BlockState oldState) {}

    @Override
    public boolean canExecute(MobActionContext context, BuildRequest request)
    {
        BlockState blockState = context.level().getBlockState(request.pos());

        boolean canReplaced = context.level().isLoaded(request.pos()) && blockState.canBeReplaced();
        boolean cooldownPassed = context.aiTimers().buildCooldownPassed(context.level().getGameTime());
        boolean canMobBuild = !context.configSnapshot().runtime().ignoreBuildEntityIdMatcher().matches(context.mobId());

        return cooldownPassed && canReplaced && canMobBuild;
    }

    @Override
    public void execute(MobActionContext context, BuildRequest request)
    {
        BlockState oldState = context.level().getBlockState(request.pos());
        BlockState placedState = request.bridgeBlock().defaultBlockState();

        if (context.level().setBlockAndUpdate(request.pos(), request.bridgeBlock().defaultBlockState()))
        {
            Constants.EVENT_BUS.post(new OnAnyBlockPlacedEvent(context.level(), request.pos(), context.configSnapshot(), placedState, oldState));
        }

        context.aiTimers().setBuildCooldownUntil(context.level().getGameTime() + SecondsToTicksUtility.toTicks(context.configSnapshot().data().balance.cooldowns.buildCooldown, 1));
    }
}
