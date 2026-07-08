package com.tik.zbb;

import net.fabricmc.api.ModInitializer;
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

        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> MainCommon.onReload());
    }
}
