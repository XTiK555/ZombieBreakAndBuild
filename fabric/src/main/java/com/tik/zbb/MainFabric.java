package com.tik.zbb;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
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

        ServerTickEvents.END_WORLD_TICK.register(MainCommon::onLevelTick);
    }
}
