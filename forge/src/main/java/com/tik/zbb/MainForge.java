package com.tik.zbb;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(Constants.MOD_ID)
public class MainForge
{
    public MainForge()
    {
        MainCommon.init();

        TickEvent.LevelTickEvent.Post.BUS.addListener(this::onLevelTick);
        TickEvent.ServerTickEvent.Post.BUS.addListener(this::onServerTick);
        ServerStartingEvent.BUS.addListener(this::onServerStarting);
        ServerStoppingEvent.BUS.addListener(this::onServerStopping);
        EntityJoinLevelEvent.BUS.addListener(this::onJoin);
        RegisterCommandsEvent.BUS.addListener(this::onRegisterCommands);
    }

    private void onLevelTick(TickEvent.LevelTickEvent.Post event)
    {
        if (!(event.level() instanceof ServerLevel serverLevel)) return;

        MainCommon.onLevelTick(serverLevel);
    }

    private void onServerTick(TickEvent.ServerTickEvent.Post event)
    {
        MainCommon.onServerTick(event.server());
    }

    private void onServerStopping(ServerStoppingEvent event)
    {
        MainCommon.onServerStopping(event.getServer());
    }

    private void onServerStarting(ServerStartingEvent event)
    {
        MainCommon.onServerStarting(event.getServer());
    }

    private void onJoin(EntityJoinLevelEvent event)
    {
        if (!(event.getEntity() instanceof Mob mob)) return;

        MainCommon.onJoin(mob);
    }

    private void onRegisterCommands(RegisterCommandsEvent event)
    {
        MainCommon.registerCommands(event.getDispatcher());
    }
}
