package com.tik.zbb.ai.action.actions.breakk;

import com.tik.zbb.Constants;
import com.tik.zbb.ai.action.IMobAction;
import com.tik.zbb.ai.action.MobActionContext;
import com.tik.zbb.blockstorage.BlockStorages;
import com.tik.zbb.blockstorage.storages.damage.DamageBlockStorageEntry;
import com.tik.zbb.config.ConfigSnapshot;
import com.tik.zbb.utilities.BlockDamageCalculator;
import com.tik.zbb.utilities.BlockHealthCalculator;
import com.tik.zbb.utilities.SecondsToTicksUtility;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.block.state.BlockState;

public class BreakAction implements IMobAction<BreakRequest>
{
    public record OnAnyBlockWillBrokeEvent(ServerLevel level, BlockPos pos, BlockState state, ConfigSnapshot configSnapshot, PathfinderMob mob) {}

    public record OnAnyBlockBrokenEvent(ServerLevel level, BlockPos pos, BlockState oldState, ConfigSnapshot configSnapshot, PathfinderMob mob) {}

    public record OnAnyBlockFailedToBrokeEvent(ServerLevel level, BlockPos pos, BlockState state, ConfigSnapshot configSnapshot, PathfinderMob mob) {}

    public record OnAnyBlockHit(ServerLevel level, BlockPos pos, BlockState state, ConfigSnapshot configSnapshot, PathfinderMob mob, int blockHealth,
                                int newDamage, DamageBlockStorageEntry storageEntry) {}
    
    @Override
    public boolean canExecute(MobActionContext context, BreakRequest request)
    {
        if (!context.aiTimers().breakCooldownPassed(context.level().getGameTime())) return false;
        if (!context.level().isLoaded(request.pos())) return false;
        if (context.configSnapshot().game().ai().ignoreBreakEntityIdMatcher().matches(context.mobId(), context.mob().getType().getCategory())) return false;
        if (BlockStorages.BUILD_PROTECTION_MANAGER.contains(context.level(), request.pos())) return false;

        BlockState blockState = context.level().getBlockState(request.pos());

        if (blockState.isAir()) return false;

        return !BlockHealthCalculator.isUnbreakableBlock(blockState, request.pos(), context.level(), context.configSnapshot());
    }

    @Override
    public boolean execute(MobActionContext context, BreakRequest request)
    {
        BlockState state = context.level().getBlockState(request.pos());
        int blockHealth = BlockHealthCalculator.getBlockHealth(state, request.pos(), context.level(), context.configSnapshot());
        int newDamage = BlockDamageCalculator.getDamageToBlocks(context.mob(), state, context.configSnapshot());
        int totalDamage = saturatingAdd(
                BlockStorages.DAMAGE_MANAGER.getTotalBlockDamage(context.level(), request.pos()),
                newDamage
        );

        boolean succeeded = true;
        if (totalDamage >= blockHealth)
        {
            boolean dropLoot = !context.configSnapshot().game().blockRestoration().brokenBlocksRestoring();

            boolean destroyed = false;
            try
            {
                Constants.EVENT_BUS.post(new OnAnyBlockWillBrokeEvent(context.level(), request.pos(), state, context.configSnapshot(), context.mob()));

                state = context.level().getBlockState(request.pos());

                destroyed = context.level().destroyBlock(request.pos(), dropLoot);

                if (destroyed)
                {
                    BlockStorages.DAMAGE_MANAGER.removeRecord(context.level(), request.pos());
                }
            }
            finally
            {
                Constants.EVENT_BUS.post(destroyed
                        ? new OnAnyBlockBrokenEvent(context.level(), request.pos(), state, context.configSnapshot(), context.mob())
                        : new OnAnyBlockFailedToBrokeEvent(context.level(), request.pos(), state, context.configSnapshot(), context.mob()));
            }
            succeeded = destroyed;
        }
        else
        {
            DamageBlockStorageEntry addedDamageStorageEntry = BlockStorages.DAMAGE_MANAGER.addDamageRecord(context.level(), request.pos(), totalDamage);

            Constants.EVENT_BUS.post(new OnAnyBlockHit(context.level(), request.pos(), state, context.configSnapshot(), context.mob(), blockHealth, newDamage, addedDamageStorageEntry));
        }


        context.aiTimers().setBreakCooldownUntil(context.level().getGameTime() + SecondsToTicksUtility.toTicks(context.configSnapshot().game().balance().cooldowns().breakCooldown(), 1));
        return succeeded;
    }

    private int saturatingAdd(int left, int right)
    {
        return (int) Math.min(Integer.MAX_VALUE, (long) left + right);
    }
}
