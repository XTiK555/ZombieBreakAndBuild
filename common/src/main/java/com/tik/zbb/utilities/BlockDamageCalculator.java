package com.tik.zbb.utilities;

import com.tik.zbb.config.ConfigRuntime;
import com.tik.zbb.config.ConfigSnapshot;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class BlockDamageCalculator
{
    public static int getDamageToBlocks(PathfinderMob mob, BlockState state, ConfigSnapshot configSnapshot)
    {
        ConfigRuntime.BlockDamage blockDamageCfg = configSnapshot.game().balance().blockDamage();
        int baseDamage = blockDamageCfg.damageToBlocks();
        double damage = baseDamage * getHitboxSizeMultiplier(mob, blockDamageCfg) * getItemMultiplier(mob, state, blockDamageCfg);

        if (damage >= Integer.MAX_VALUE) return Integer.MAX_VALUE;
        if (baseDamage == 0) return 0;

        return Math.max(1, (int) Math.round(damage));
    }

    private static double getHitboxSizeMultiplier(PathfinderMob mob, ConfigRuntime.BlockDamage blockDamageCfg)
    {
        double zombieWidth = EntityTypes.ZOMBIE.getDimensions().width();
        double zombieHeight = EntityTypes.ZOMBIE.getDimensions().height();
        double baseVolume = zombieWidth * zombieWidth * zombieHeight;
        double mobVolume = mob.getBbWidth() * mob.getBbWidth() * mob.getBbHeight();

        return Math.pow(mobVolume / baseVolume, blockDamageCfg.hitboxSizeMultiplierExponent());
    }

    private static double getItemMultiplier(PathfinderMob mob, BlockState state, ConfigRuntime.BlockDamage blockDamageCfg)
    {
        ItemStack mainHandItem = mob.getMainHandItem();
        ItemStack offhandItem = mob.getOffhandItem();
        double destroySpeed = Math.max(mainHandItem.getDestroySpeed(state), offhandItem.getDestroySpeed(state));

        return Math.pow(destroySpeed, blockDamageCfg.itemDamageMultiplierExponent());
    }
}
