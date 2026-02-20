package com.tik.zbb;

import com.tik.zbb.config.ConfigManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

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
    public void onJoin(EntityJoinLevelEvent event)
    {
        if (!(event.getEntity() instanceof Mob mob)) return;

        MainCommon.onJoin(mob);
    }

//    @SubscribeEvent
//    public static void onAddReloadListeners(AddServerReloadListenersEvent event)
//    {
//        event.addListener(Objects.requireNonNull(Identifier.tryParse(Constants.MOD_ID + ":config_reload")), (sharedState, prepareExecutor, preparationBarrier, applyExecutor)
//                -> preparationBarrier
//                .wait(CompletableFuture.completedFuture(null))
//                .thenRunAsync(ConfigManager::reload, applyExecutor));
//    }

    @SubscribeEvent
    public void onAddReloadListeners(AddServerReloadListenersEvent event)
    {
        event.addListener(Objects.requireNonNull(Identifier.tryParse(Constants.MOD_ID + ":config_reload")), new SimplePreparableReloadListener<Void>()
        {
            @Override
            protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler)
            {
                return null;
            }

            @Override
            protected void apply(Void object, ResourceManager resourceManager, ProfilerFiller profiler)
            {
                ConfigManager.reload();
            }
        });
    }
}