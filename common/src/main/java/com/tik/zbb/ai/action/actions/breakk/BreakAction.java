package com.tik.zbb.ai.action.actions.breakk;

import com.tik.zbb.Constants;
import com.tik.zbb.ai.action.IMobAction;
import com.tik.zbb.ai.action.MobActionContext;
import com.tik.zbb.blockstorage.BlockStorages;
import com.tik.zbb.config.ConfigData;
import com.tik.zbb.config.ConfigSnapshot;
import com.tik.zbb.utilities.SecondsToTicksUtility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class BreakAction implements IMobAction<BreakRequest>
{
    public record OnAnyBlockWillBrokeEvent(ServerLevel level, BlockPos pos, BlockState state,
                                           ConfigSnapshot configSnapshot, PathfinderMob mob, int blockId) {}

    public record OnAnyBlockBrokenEvent(ServerLevel level, BlockPos pos, BlockState oldState,
                                        ConfigSnapshot configSnapshot, PathfinderMob mob, int blockId) {}

    public record OnAnyBlockFailedToBrokeEvent(ServerLevel level, BlockPos pos, BlockState state,
                                               ConfigSnapshot configSnapshot, PathfinderMob mob, int blockId) {}

    public record OnAnyBlockHit(ServerLevel level, BlockPos pos, BlockState state,
                                ConfigSnapshot configSnapshot, PathfinderMob mob, int totalDamage, int blockHealth,
                                int newDamage, int blockId) {}


    @Override
    public boolean canExecute(MobActionContext context, BreakRequest request)
    {
        boolean isAir = context.level().getBlockState(request.pos()).isAir();
        boolean cooldownPassed = context.aiTimers().breakCooldownPassed(context.level().getGameTime());
        boolean notRecentlyBuilt = !BlockStorages.BUILD_PROTECTION_MANAGER.contains(context.level(), request.pos());
        boolean unbreakable = getBlockHealth(request.pos(), context.level(), context.configSnapshot().data()) == Integer.MAX_VALUE;
        boolean canMobBreak = !context.configSnapshot().data().ignoreBreakEntityIdSet.contains(context.mobId());

        return cooldownPassed && notRecentlyBuilt && !isAir && !unbreakable && canMobBreak;
    }

    @Override
    public void execute(MobActionContext context, BreakRequest request)
    {
        BlockState state = context.level().getBlockState(request.pos());
        int blockId = BlockStorages.ID_MANAGER.getOrCreate(context.level(), request.pos());
        int blockHealth = getBlockHealth(request.pos(), context.level(), context.configSnapshot().data());
        int newDamage = getDamageToBlocks(context, request.pos());
        int totalDamage = BlockStorages.DAMAGE_MANAGER.getTotalBlockDamage(context.level(), request.pos()) + newDamage;

        if (totalDamage >= blockHealth)
        {
            boolean dropLoot = !context.configSnapshot().data().blockRestoration.brokenBlocksRestoring;

            Constants.EVENT_BUS.post(new OnAnyBlockWillBrokeEvent(context.level(), request.pos(), state, context.configSnapshot(), context.mob(), blockId));

            if (context.level().destroyBlock(request.pos(), dropLoot))
            {
                Constants.EVENT_BUS.post(new OnAnyBlockBrokenEvent(context.level(), request.pos(), state, context.configSnapshot(), context.mob(), blockId));
            }
            else
            {
                Constants.EVENT_BUS.post(new OnAnyBlockFailedToBrokeEvent(context.level(), request.pos(), state, context.configSnapshot(), context.mob(), blockId));
            }
        }
        else
        {
            Constants.EVENT_BUS.post(new OnAnyBlockHit(context.level(), request.pos(), state, context.configSnapshot(), context.mob(), totalDamage, blockHealth, newDamage, blockId));
        }


        context.aiTimers().setBreakCooldownUntil(context.level().getGameTime() + SecondsToTicksUtility.toTicks(context.configSnapshot().data().balance.cooldowns.breakCooldown, 1));
    }

    private int getBlockHealth(BlockPos blockPos, ServerLevel level, ConfigData configData)
    {
        BlockState blockState = level.getBlockState(blockPos);
        Registry<Block> blockRegistry = level.registryAccess().lookupOrThrow(Registries.BLOCK);
        Identifier blockId = blockRegistry.getKey(blockState.getBlock());
        Integer blockHealthOverride = configData.blockHealthOverrideMap.get(blockId);
        float hardness = blockState.getDestroySpeed(level, blockPos);
        double health = Math.pow(hardness, configData.balance.blockDamage.blockHardnessContrast) * configData.balance.blockDamage.blockHardnessMultiplier;

        if (blockHealthOverride != null) return blockHealthOverride;
        if (hardness < 0) return Integer.MAX_VALUE;
        if (health >= Integer.MAX_VALUE) return Integer.MAX_VALUE;

        return (int) Math.round(health);
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

        double zombieWidth = EntityTypes.ZOMBIE.getDimensions().width();
        double zombieHeight = EntityTypes.ZOMBIE.getDimensions().height();
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
        float toolMultiplier = (float) (1.0 + (destroySpeed - 1.0) * context.configSnapshot().data().balance.blockDamage.itemDamageMultiplierStrength);

        return toolMultiplier;
    }
}
