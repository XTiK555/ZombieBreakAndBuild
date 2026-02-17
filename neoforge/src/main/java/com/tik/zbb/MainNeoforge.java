package com.tik.zbb;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

@Mod(Constants.MOD_ID)
public class MainNeoforge
{
    public MainNeoforge(IEventBus eventBus)
    {
        MainCommon.init();

        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onLevelTick(LevelTickEvent.Post event)
    {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;

        MainCommon.onLevelTick(serverLevel);
    }

    @SubscribeEvent
    public void onJoin(EntityJoinLevelEvent event)
    {
        if (!(event.getEntity() instanceof Mob mob)) return;

        MainCommon.onJoin(mob);
    }
}