package com.tik.zbb;

import com.tik.zbb.goals.AlwaysSeeNearestPlayerGoal;
import com.tik.zbb.goals.BreakAndBuildGoal;
import com.tik.zbb.goals.ThroughWallsNearestTargetGoal;
import com.tik.zbb.utilities.SecondsToTicksUtility;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
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
    public void onLevelTick(TickEvent.LevelTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.level instanceof ServerLevel sl)) return;

        long now = sl.getGameTime();
        int interval = 10;

        if (now % interval == 0)
        {
            long damageTtl = SecondsToTicksUtility.toTicks(Config.DAMAGE_STORE_TIME.get());
            long buildTtl = SecondsToTicksUtility.toTicks(Config.BUILT_BLOCKS_PROTECTION_TIME.get());

            BlockStorage.cleanUpDamageData(sl, damageTtl);
            BlockStorage.cleanUpBuildData(sl, buildTtl);
        }
    }

    @SubscribeEvent
    public void onJoin(EntityJoinLevelEvent event)
    {
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (!shouldApplyTo(mob)) return;

        if (mob instanceof PathfinderMob PFMob)
        {
            AttributeInstance followRangeAttribute = mob.getAttribute(Attributes.FOLLOW_RANGE);
            int configTargetSearchRadius = Config.TARGET_SEARCH_RADIUS.get();
            boolean isAttackingAllEntities = Config.ATTACK_ALL_ENTITIES.get();

            PFMob.goalSelector.addGoal(2, new BreakAndBuildGoal(PFMob));
            PFMob.targetSelector.addGoal(2, new AlwaysSeeNearestPlayerGoal(PFMob));

            if (isAttackingAllEntities)
            {
                PFMob.targetSelector.addGoal(1, new ThroughWallsNearestTargetGoal<>(PFMob, LivingEntity.class));
            }
            else
            {
                PFMob.targetSelector.addGoal(1, new ThroughWallsNearestTargetGoal<>(PFMob, Player.class));
            }
            if (followRangeAttribute != null && followRangeAttribute.getBaseValue() < configTargetSearchRadius)
            {
                followRangeAttribute.setBaseValue(configTargetSearchRadius);
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
            Config.rebuildSets();
        }
    }
}
