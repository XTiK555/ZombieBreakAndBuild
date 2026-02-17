package com.tik.zbb;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(Constants.MOD_ID)
public class MainForge
{
    public MainForge()
    {
        MainCommon.init();

        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onLevelTick(TickEvent.LevelTickEvent.Post event)
    {
        if (!(event.level() instanceof ServerLevel serverLevel)) return;

        MainCommon.onLevelTick(serverLevel);
    }

    @SubscribeEvent
    public void onJoin(EntityJoinLevelEvent event)
    {
        if (!(event.getEntity() instanceof Mob mob)) return;

        MainCommon.onJoin(mob);
    }
}