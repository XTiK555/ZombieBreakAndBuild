package com.tik.zbb;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
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
        ServerStoppingEvent.BUS.addListener(this::onServerStopping);
        EntityJoinLevelEvent.BUS.addListener(this::onJoin);
        AddReloadListenerEvent.BUS.addListener(this::onAddReloadListeners);
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

    private void onJoin(EntityJoinLevelEvent event)
    {
        if (!(event.getEntity() instanceof Mob mob)) return;

        MainCommon.onJoin(mob);
    }

    private void onAddReloadListeners(AddReloadListenerEvent event)
    {
        event.addListener((state, backgroundExecutor, preparationBarrier, gameExecutor) ->
                preparationBarrier.wait(Unit.INSTANCE).thenRunAsync(MainCommon::onReload, gameExecutor)
        );
    }

    private void onRegisterCommands(RegisterCommandsEvent event)
    {
        MainCommon.registerCommands(event.getDispatcher());
    }
}
