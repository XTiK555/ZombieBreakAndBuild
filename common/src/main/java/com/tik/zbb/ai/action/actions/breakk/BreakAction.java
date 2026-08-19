package com.tik.zbb.ai.action.actions.breakk;

import com.tik.zbb.Constants;
import com.tik.zbb.ai.action.IMobAction;
import com.tik.zbb.ai.action.MobActionContext;
import com.tik.zbb.blockstorage.BlockStorages;
import com.tik.zbb.config.ConfigGame;
import com.tik.zbb.config.ConfigSnapshot;
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

    public record OnAnyBlockHit(ServerLevel level, BlockPos pos, BlockState state,
                                ConfigSnapshot configSnapshot, PathfinderMob mob, int totalDamage, int blockHealth,
                                int newDamage, int blockId) {}


    @Override
    public boolean canExecute(MobActionContext context, BreakRequest request)
    {
        if (!context.aiTimers().breakCooldownPassed(context.level().getGameTime())) return false;
        if (!context.level().isLoaded(request.pos())) return false;
        if (context.configSnapshot().game().ai().ignoreBreakEntityIdMatcher()
                .matches(context.mobId(), context.mob().getType().getCategory())) return false;
        if (BlockStorages.BUILD_PROTECTION_MANAGER.contains(context.level(), request.pos())) return false;

        BlockState state = context.level().getBlockState(request.pos());
        if (state.isAir()) return false;

        return getBlockHealth(state, request.pos(), context.level(), context.configSnapshot()) != Integer.MAX_VALUE;
    }

    @Override
    public boolean execute(MobActionContext context, BreakRequest request)
    {
        BlockState state = context.level().getBlockState(request.pos());
        int blockHealth = getBlockHealth(state, request.pos(), context.level(), context.configSnapshot());
        int newDamage = getDamageToBlocks(context, state);
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
            int blockId = BlockStorages.ID_MANAGER.getOrCreate(context.level(), request.pos());

            BlockStorages.DAMAGE_MANAGER.addDamageRecord(context.level(), request.pos(), totalDamage, blockId);

            Constants.EVENT_BUS.post(new OnAnyBlockHit(context.level(), request.pos(), state, context.configSnapshot(), context.mob(), totalDamage, blockHealth, newDamage, blockId));
        }


        context.aiTimers().setBreakCooldownUntil(context.level().getGameTime() + SecondsToTicksUtility.toTicks(context.configSnapshot().game().balance().cooldowns().breakCooldown(), 1));
        return succeeded;
    }

    private int getBlockHealth(BlockPos blockPos, ServerLevel level, ConfigSnapshot configSnapshot)
    {
        return getBlockHealth(level.getBlockState(blockPos), blockPos, level, configSnapshot);
    }

    private int getBlockHealth(BlockState blockState, BlockPos blockPos, ServerLevel level, ConfigSnapshot configSnapshot)
    {
        ConfigGame.BlockDamage blockDamageCfg = configSnapshot.game().balance().blockDamage();
        Integer blockHealthOverride = blockDamageCfg.blockHealthOverrideMap().get(blockState.getBlock());
        if (blockHealthOverride != null) return blockHealthOverride;

        float hardness = blockState.getDestroySpeed(level, blockPos);
        if (hardness < 0) return Integer.MAX_VALUE;
        if (exceedsMaximumBreakableHardness(hardness, blockDamageCfg)) return Integer.MAX_VALUE;

        double health = Math.pow(hardness, blockDamageCfg.blockHardnessContrast()) * blockDamageCfg.blockHardnessMultiplier();
        if (health >= Integer.MAX_VALUE) return Integer.MAX_VALUE;

        return Math.max(1, (int) Math.round(health));
    }

    private int getDamageToBlocks(MobActionContext context, BlockState state)
    {
        int baseDamage = context.configSnapshot().game().balance().blockDamage().damageToBlocks();
        double hitboxMultiplier = getHitboxSizeMultiplier(context);
        double itemMultiplier = getItemMultiplier(context, state);

        double damage = baseDamage * hitboxMultiplier * itemMultiplier;
        if (damage >= Integer.MAX_VALUE)
        {
            return Integer.MAX_VALUE;
        }

        return Math.max(0, (int) Math.round(damage));
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

    private double getItemMultiplier(MobActionContext context, BlockState state)
    {
        ItemStack mainHandItem = context.mob().getMainHandItem();
        ItemStack offhandItem = context.mob().getOffhandItem();
        float mainHandDestroySpeed = mainHandItem.getDestroySpeed(state);
        float offhandDestroySpeed = offhandItem.getDestroySpeed(state);
        float destroySpeed = Math.max(mainHandDestroySpeed, offhandDestroySpeed);
        double toolMultiplier = Math.pow(
                destroySpeed,
                context.configSnapshot().game().balance().blockDamage().itemDamageMultiplierStrength()
        );

        return toolMultiplier;
    }

    private boolean exceedsMaximumBreakableHardness(float hardness, ConfigGame.BlockDamage blockDamage)
    {
        return blockDamage.maximumBreakableBlockHardness() > 0.0f && hardness > blockDamage.maximumBreakableBlockHardness();
    }
}
