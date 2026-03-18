package com.tik.zbb.mixin;

import com.tik.zbb.config.ConfigManager;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Zombie.class)
public abstract class ZombieMixin extends Monster
{
    protected ZombieMixin(EntityType<? extends Monster> type, Level level)
    {
        super(type, level);
    }

    @Inject(method = "populateDefaultEquipmentSlots", at = @At("HEAD"), cancellable = true)
    private void zbb$populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty, CallbackInfo ci)
    {
        if (ConfigManager.getConfigSnapshot().data().balance.additionalZombieEquipmentSpawnChance <= 0) return;

        super.populateDefaultEquipmentSlots(random, difficulty);

        float vanillaChance = difficulty.getDifficulty() == Difficulty.HARD ? 0.05F : 0.01F;
        float additionalChance = Mth.clamp(ConfigManager.getConfigSnapshot().data().balance.additionalZombieEquipmentSpawnChance, 0, 100) / 100.0F;
        float finalChance = vanillaChance + (1.0F - vanillaChance) * additionalChance;

        if (random.nextFloat() < finalChance)
        {
            int i = random.nextInt(6);
            if (i == 0)
            {
                this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            }
            else if (i == 1)
            {
                this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SPEAR));
            }
            else if (i == 2)
            {
                this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));
            }
            else
            {
                this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SHOVEL));
            }
        }

        ci.cancel();
    }
}
