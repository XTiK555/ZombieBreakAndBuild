package com.tik.zbb.ai.action.actions.breakk;

import com.tik.zbb.Constants;
import com.tik.zbb.ai.action.IMobAction;
import com.tik.zbb.ai.action.MobActionContext;
import com.tik.zbb.blockstorage.BlockStorages;
import com.tik.zbb.blockstorage.storages.damage.DamageBlockStorageEntry;
import com.tik.zbb.config.ConfigSnapshot;
import com.tik.zbb.utilities.BlockHealthCalculator;
import com.tik.zbb.utilities.SecondsToTicksUtility;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class BreakAction implements IMobAction<BreakRequest>
{
    public record OnAnyBlockWillBrokeEvent(ServerLevel level, BlockPos pos, BlockState state,
                                           ConfigSnapshot configSnapshot, PathfinderMob mob) {}

    public record OnAnyBlockBrokenEvent(ServerLevel level, BlockPos pos, BlockState oldState,
                                        ConfigSnapshot configSnapshot, PathfinderMob mob) {}

    public record OnAnyBlockFailedToBrokeEvent(ServerLevel level, BlockPos pos, BlockState state,
                                               ConfigSnapshot configSnapshot, PathfinderMob mob) {}

    public record OnAnyBlockHit(ServerLevel level, BlockPos pos, BlockState state, ConfigSnapshot configSnapshot, PathfinderMob mob, int blockHealth,
                                int newDamage, DamageBlockStorageEntry storageEntry) {}


    @Override
    public boolean canExecute(MobActionContext context, BreakRequest request)
    {
        if (!context.level().isLoaded(request.pos())) return false;

        boolean isAir = context.level().getBlockState(request.pos()).isAir();
        boolean cooldownPassed = context.aiTimers().breakCooldownPassed(context.level().getGameTime());
        boolean notRecentlyBuilt = !BlockStorages.BUILD_PROTECTION_MANAGER.contains(context.level(), request.pos());
        boolean unbreakable = BlockHealthCalculator.getBlockHealth(request.pos(), context.level(), context.configSnapshot()) == Integer.MAX_VALUE;
        boolean canMobBreak = !context.configSnapshot().game().ai().ignoreBreakEntityIdMatcher()
                .matches(context.mobId(), context.mob().getType().getCategory());

        return cooldownPassed && notRecentlyBuilt && !isAir && !unbreakable && canMobBreak;
    }

    @Override
    public boolean execute(MobActionContext context, BreakRequest request)
    {
        BlockState state = context.level().getBlockState(request.pos());
        int blockHealth = BlockHealthCalculator.getBlockHealth(request.pos(), context.level(), context.configSnapshot());
        int newDamage = getDamageToBlocks(context, request.pos());
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


    private int getDamageToBlocks(MobActionContext context, BlockPos breakPos)
    {
        int baseDamage = context.configSnapshot().game().balance().blockDamage().damageToBlocks();
        double hitboxMultiplier = getHitboxSizeMultiplier(context);
        double itemMultiplier = getItemMultiplier(context, breakPos);

        double damage = baseDamage * hitboxMultiplier * itemMultiplier;
        if (damage >= Integer.MAX_VALUE)
        {
            return Integer.MAX_VALUE;
        }

        return Math.max(0, (int) Math.round(baseDamage * hitboxMultiplier * itemMultiplier));
    }

    private int saturatingAdd(int left, int right)
    {
        return (int) Math.min(Integer.MAX_VALUE, (long) left + right);
    }

    private double getHitboxSizeMultiplier(MobActionContext context)
    {
        double width = context.mob().getBbWidth();
        double height = context.mob().getBbHeight();

        double zombieWidth = EntityTypes.ZOMBIE.getDimensions().width();
        double zombieHeight = EntityTypes.ZOMBIE.getDimensions().height();
        double baseVolume = zombieWidth * zombieWidth * zombieHeight;

        double mobVolume = width * width * height;

        double hitboxRatio = mobVolume / baseVolume;
        double finalMultiplier = Math.pow(
                hitboxRatio,
                context.configSnapshot().game().balance().blockDamage().hitboxSizeMultiplierStrength()
        );

        return finalMultiplier;
    }

    private double getItemMultiplier(MobActionContext context, BlockPos breakPos)
    {
        ItemStack mainHandItem = context.mob().getMainHandItem();
        ItemStack offhandItem = context.mob().getOffhandItem();
        BlockState state = context.level().getBlockState(breakPos);
        float mainHandDestroySpeed = mainHandItem.getDestroySpeed(state);
        float offhandDestroySpeed = offhandItem.getDestroySpeed(state);
        float destroySpeed = Math.max(mainHandDestroySpeed, offhandDestroySpeed);
        double toolMultiplier = Math.pow(
                destroySpeed,
                context.configSnapshot().game().balance().blockDamage().itemDamageMultiplierStrength()
        );

        return toolMultiplier;
    }
}
