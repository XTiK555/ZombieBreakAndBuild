package com.tik.zbb;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@Mod(Constants.MOD_ID)
public class MainNeoForge
{
    public MainNeoForge(IEventBus eventBus)
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
    public void onServerTick(ServerTickEvent.Post event)
    {
        MainCommon.onServerTick(event.getServer());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event)
    {
        MainCommon.onServerStopping(event.getServer());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        MainCommon.onServerStarting(event.getServer());
    }

    @SubscribeEvent
    public void onJoin(EntityJoinLevelEvent event)
    {
        if (!(event.getEntity() instanceof Mob mob)) return;

        MainCommon.onJoin(mob);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event)
    {
        MainCommon.registerCommands(event.getDispatcher());
    }
}
