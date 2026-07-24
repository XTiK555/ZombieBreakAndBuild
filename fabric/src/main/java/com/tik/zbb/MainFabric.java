package com.tik.zbb;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.world.entity.Mob;

public class MainFabric implements ModInitializer
{
    @Override
    public void onInitialize()
    {
        MainCommon.init();

        ServerEntityEvents.ENTITY_LOAD.register((entity, serverLevel) ->
        {
            if (entity instanceof Mob mob)
            {
                MainCommon.onJoin(mob);
            }
        });

        ServerTickEvents.END_LEVEL_TICK.register(MainCommon::onLevelTick);

        ServerTickEvents.END_SERVER_TICK.register(MainCommon::onServerTick);

        ServerLifecycleEvents.SERVER_STOPPING.register(MainCommon::onServerStopping);

        ServerLifecycleEvents.SERVER_STARTING.register(MainCommon::onServerStarting);

        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> MainCommon.registerCommands(dispatcher));
    }
}
