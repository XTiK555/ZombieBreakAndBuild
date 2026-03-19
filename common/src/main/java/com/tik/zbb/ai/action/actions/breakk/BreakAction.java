package com.tik.zbb.ai.action.actions.breakk;

import com.tik.zbb.Constants;
import com.tik.zbb.ai.action.IMobAction;
import com.tik.zbb.ai.action.MobActionContext;
import com.tik.zbb.blockstorage.BlockStorages;
import com.tik.zbb.config.ConfigSnapshot;
import com.tik.zbb.utilities.SecondsToTicksUtility;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class BreakAction implements IMobAction<BreakRequest>
{
    private static final int MAX_BLOCK_HARDNESS_FOR_HEALTH = 50;
    private static final float BLOCK_HEALTH_PER_HARDNESS = 6.0f;
    private static final int MIN_BLOCK_HEALTH = 1;
    private static final float MIN_TOOL_DESTROY_SPEED = 1.0f;
    private static final float MAX_TOOL_DESTROY_SPEED = 30.0f;

    @Override
    public boolean canExecute(MobActionContext context, BreakRequest request)
    {
        boolean isAir = context.level().getBlockState(request.pos()).isAir();
        boolean cooldownPassed = context.aiTimers().breakCooldownPassed(context.level().getGameTime());
        boolean notRecentlyBuilt = !BlockStorages.BUILD_PROTECTION.contains(context.level(), request.pos());
        boolean unbreakable = getBlockHealth(request.pos(), context.level()) == Integer.MAX_VALUE;
        boolean canMobBreak = !context.configSnapshot().data().ignoreBreakEntityIdSet.contains(context.mobId());

        return cooldownPassed && notRecentlyBuilt && !isAir && !unbreakable && canMobBreak;
    }

    @Override
    public void execute(MobActionContext context, BreakRequest request)
    {
        BlockState state = context.level().getBlockState(request.pos());
        int id = BlockStorages.ID.getOrCreate(context.level(), request.pos());
        int blockHealth = getBlockHealth(request.pos(), context.level());
        int damageToBlocks = getDamageToBlocks(context, request.pos());
        int totalDamage = BlockStorages.DAMAGE.addDamageData(context.level(), request.pos(), damageToBlocks, id);

        if (totalDamage >= blockHealth)
        {
            boolean dropLoot = !context.configSnapshot().data().blockReturning.brokenBlocksRestoring;

            Constants.EVENT_BUS.post(new OnAnyBlockWillBrokeEvent(context.level(), request.pos(), state, context.configSnapshot()));
            if (context.level().destroyBlock(request.pos(), dropLoot))
            {
                Constants.EVENT_BUS.post(new OnAnyBlockBrokenEvent(context.level(), request.pos(), state, context.configSnapshot()));
            }
            else
            {
                Constants.EVENT_BUS.post(new OnAnyBlockFailedToBrokeEvent(context.level(), request.pos(), state, context.configSnapshot()));
            }
        }
        else
        {
            int stage = Math.min(9, (totalDamage * 10) / blockHealth);

            context.level().destroyBlockProgress(id, request.pos(), stage);
            context.level().levelEvent(2001, request.pos(), Block.getId(state)); // particles and sound
        }

        context.aiTimers().setBreakCooldownUntil(context.level().getGameTime() + SecondsToTicksUtility.toTicks(context.configSnapshot().data().balance.cooldowns.breakCooldown, 1));
    }

    private int getBlockHealth(BlockPos blockPos, ServerLevel level)
    {
        BlockState blockState = level.getBlockState(blockPos);
        float hardness = blockState.getDestroySpeed(level, blockPos);

        if (hardness < 0) return Integer.MAX_VALUE;

        hardness = Math.min(hardness, MAX_BLOCK_HARDNESS_FOR_HEALTH);
        return Math.max(MIN_BLOCK_HEALTH, (int) (hardness * BLOCK_HEALTH_PER_HARDNESS));
    }

    private int getDamageToBlocks(MobActionContext context, BlockPos breakPos)
    {
        int baseDamage = context.configSnapshot().data().balance.blockDamage.damageToBlocks;
        double hitboxMultiplier = getHitboxSizeMultiplier(context);
        double itemMultiplier = getItemMultiplier(context, breakPos);

        return Math.max(1, (int) Math.round(baseDamage * hitboxMultiplier * itemMultiplier));
    }

    private double getHitboxSizeMultiplier(MobActionContext context)
    {
        double width = context.mob().getBbWidth();
        double height = context.mob().getBbHeight();

        double zombieWidth = EntityType.ZOMBIE.getDimensions().width;
        double zombieHeight = EntityType.ZOMBIE.getDimensions().height;
        double baseVolume = zombieWidth * zombieWidth * zombieHeight;

        double mobVolume = width * width * height;

        double hitboxMultiplier = mobVolume / baseVolume;
        double finalMultiplier = 1.0D + (hitboxMultiplier - 1.0D) * context.configSnapshot().data().balance.blockDamage.hitboxSizeMultiplierStrength;

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

        float toolMultiplier = Mth.clamp(destroySpeed, MIN_TOOL_DESTROY_SPEED, MAX_TOOL_DESTROY_SPEED);
        float finalMultiplier = (float) (1.0 + (toolMultiplier - 1.0) * context.configSnapshot().data().balance.blockDamage.itemDamageMultiplierStrength);

        return finalMultiplier;
    }

    public record OnAnyBlockWillBrokeEvent(ServerLevel level, BlockPos pos, BlockState state,
                                           ConfigSnapshot configSnapshot) {}

    public record OnAnyBlockBrokenEvent(ServerLevel level, BlockPos pos, BlockState oldState,
                                        ConfigSnapshot configSnapshot) {}

    public record OnAnyBlockFailedToBrokeEvent(ServerLevel level, BlockPos pos, BlockState state,
                                               ConfigSnapshot configSnapshot) {}
}
