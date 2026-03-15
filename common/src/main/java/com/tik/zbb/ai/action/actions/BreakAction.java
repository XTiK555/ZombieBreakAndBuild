package com.tik.zbb.ai.action.actions;

import com.tik.zbb.ai.action.IMobAction;
import com.tik.zbb.ai.action.MobActionContext;
import com.tik.zbb.blockstorage.BlockStorages;
import com.tik.zbb.utilities.SecondsToTicksUtility;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Clearable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BreakAction implements IMobAction
{
    private static final int MAX_EFFECTIVE_HARDNESS = 50;
    private static final float HARDNESS_TO_HEALTH_MULTIPLIER = 6.0f;
    private static final int MIN_BLOCK_HEALTH = 1;
    private static final double DAMAGE_SCALE_EXPONENT = 0.58D;

    private BlockPos breakPos;

    @Override
    public boolean canExecute(MobActionContext context)
    {
        boolean isAir = context.level().getBlockState(breakPos).isAir();
        boolean cooldownPassed = context.aiTimers().breakCooldownPassed(context.level().getGameTime());
        boolean notRecentlyBuilt = !BlockStorages.BUILD_PROTECTION.contains(context.level(), breakPos);
        boolean unbreakable = getBlockHealth(breakPos, context.level()) == Integer.MAX_VALUE;
        boolean canMobBreak = !context.configSnapshot().data().ignoreBreakEntityIdSet.contains(context.mobId());

        return cooldownPassed && notRecentlyBuilt && !isAir && !unbreakable && canMobBreak;
    }

    @Override
    public void execute(MobActionContext context)
    {
        BlockState state = context.level().getBlockState(breakPos);
        int blockHealth = getBlockHealth(breakPos, context.level());
        int damageToBlocks = getDamageToBlocks(context);
        int totalDamage = BlockStorages.DAMAGE.addDamageData(context.level(), breakPos, damageToBlocks);

        if (totalDamage >= blockHealth)
        {
            boolean blockRestoring = context.configSnapshot().data().blockReturning.brokenBlocksRestoring;

            if (blockRestoring)
            {
                BlockStorages.BROKEN.addBrokenData(context.level(), breakPos);

                BlockEntity blockEntity = context.level().getBlockEntity(breakPos);
                if (blockEntity instanceof Clearable clearable)
                {
                    clearable.clearContent();
                    blockEntity.setChanged();
                }
            }

            BlockStorages.DAMAGE.remove(context.level(), breakPos);
            context.level().destroyBlock(breakPos, !blockRestoring);
        }
        else
        {
            int stage = Math.min(9, (totalDamage * 10) / blockHealth);
            context.level().destroyBlockProgress(breakPos.hashCode(), breakPos, stage);
            context.level().levelEvent(2001, breakPos, Block.getId(state)); // particles and sound
        }

        context.executor().tryExecuteFreezeAction();
        context.aiTimers().setBreakCooldownUntil(context.level().getGameTime() + SecondsToTicksUtility.toTicks(context.configSnapshot().data().balance.cooldowns.breakCooldown, 1));
    }

    public void setup(BlockPos breakPos)
    {
        this.breakPos = breakPos;
    }

    private int getBlockHealth(BlockPos blockPos, ServerLevel level)
    {
        BlockState blockState = level.getBlockState(blockPos);
        float hardness = blockState.getDestroySpeed(level, blockPos);

        if (hardness < 0) return Integer.MAX_VALUE;

        hardness = Math.min(hardness, MAX_EFFECTIVE_HARDNESS);
        return Math.max(MIN_BLOCK_HEALTH, (int) (hardness * HARDNESS_TO_HEALTH_MULTIPLIER));
    }

    private int getDamageToBlocks(MobActionContext context)
    {
        int baseDamage = context.configSnapshot().data().balance.blockDamage.damageToBlocks;
        double hitboxMultiplier = getHitboxSizeMultiplier(context);
        double itemMultiplier = getItemMultiplier(context);

        return Math.max(1, (int) Math.round(baseDamage * hitboxMultiplier * itemMultiplier));
    }

    private double getHitboxSizeMultiplier(MobActionContext context)
    {
        double width = context.mob().getBbWidth();
        double height = context.mob().getBbHeight();

        double zombieWidth = net.minecraft.world.entity.EntityType.ZOMBIE.getDimensions().width();
        double zombieHeight = net.minecraft.world.entity.EntityType.ZOMBIE.getDimensions().height();
        double baseVolume = zombieWidth * zombieWidth * zombieHeight;

        double mobVolume = width * width * height;
        double volumeRatio = mobVolume / baseVolume;

        double hitboxMultiplier = Math.pow(volumeRatio, DAMAGE_SCALE_EXPONENT);
        double finalMultiplier = 1.0D + (hitboxMultiplier - 1.0D) * context.configSnapshot().data().balance.blockDamage.hitboxSizeMultiplierStrength;

        return finalMultiplier;
    }

    private double getItemMultiplier(MobActionContext context)
    {
        ItemStack mainHandItem = context.mob().getMainHandItem();
        ItemStack offhandItem = context.mob().getOffhandItem();
        BlockState state = context.level().getBlockState(breakPos);
        float mainHandDestroySpeed = mainHandItem.getDestroySpeed(state);
        float offhandDestroySpeed = offhandItem.getDestroySpeed(state);
        float destroySpeed = Math.max(mainHandDestroySpeed, offhandDestroySpeed);

        float toolMultiplier = Mth.clamp(destroySpeed, 1.0f, 30.0f);
        float finalMultiplier = (float) (1.0 + (toolMultiplier - 1.0) * context.configSnapshot().data().balance.blockDamage.itemDamageMultiplierStrength);

        return finalMultiplier;
    }
}
