package com.tik.zbb;

import com.tik.zbb.goals.AlwaysSeeNearestPlayerGoal;
import com.tik.zbb.goals.BreakAndBuildGoal;
import com.tik.zbb.goals.ThroughWallsNearestTargetGoal;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Main.MODID)
public class Main
{
    public static final String MODID = "zbb";


    public Main(FMLJavaModLoadingContext context)
    {
        // register config
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        IEventBus bus = context.getModEventBus();
        bus.addListener(this::onConfigEvent);

        // event subscription
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onLevelTick(TickEvent.LevelTickEvent e)
    {
        if (e.phase != TickEvent.Phase.END) return;
        if (!(e.level instanceof ServerLevel sl)) return;

        long now = sl.getGameTime();
        int interval = 10;

        if (now % interval == 0)
        {
            BlockStorage.cleanUpDamageData(sl, Config.DAMAGE_STORE_TIME.get().longValue());
            BlockStorage.cleanUpBuildData(sl, Config.BUILT_BLOCKS_PROTECTION_TIME.get().longValue());
        }
    }

    @SubscribeEvent
    public void onJoin(EntityJoinLevelEvent event)
    {
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (!shouldApplyTo(mob)) return;

        if (mob instanceof PathfinderMob PFMob)
        {
            PFMob.goalSelector.addGoal(2, new BreakAndBuildGoal(PFMob));
            PFMob.targetSelector.addGoal(1, new ThroughWallsNearestTargetGoal<>(PFMob, LivingEntity.class));
            PFMob.targetSelector.addGoal(2, new AlwaysSeeNearestPlayerGoal(PFMob));

            AttributeInstance attributeInstance = mob.getAttribute(Attributes.FOLLOW_RANGE);
            if (attributeInstance != null)
            {
                attributeInstance.setBaseValue(Config.FOLLOW_RANGE_OVERRIDE.get());
            }
        }
    }

    private static boolean shouldApplyTo(Mob mob)
    {
        boolean applyToAllHostiles = Config.APPLY_TO_ALL_HOSTILES.get();
        boolean isHostile = mob.getType().getCategory() == MobCategory.MONSTER;

        if (applyToAllHostiles) return isHostile;
        return mob instanceof Zombie;
    }

    private void onConfigEvent(final ModConfigEvent event)
    {
        if (event.getConfig().getSpec() == Config.SPEC)
        {
            Config.rebuildDangerousBlocksSet();
        }
    }
}
