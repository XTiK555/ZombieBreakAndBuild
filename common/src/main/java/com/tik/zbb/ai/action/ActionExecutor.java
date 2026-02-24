package com.tik.zbb.ai.action;

import com.tik.zbb.ai.action.actions.BreakAction;
import com.tik.zbb.ai.action.actions.FreezeAction;
import com.tik.zbb.config.ConfigData;
import com.tik.zbb.config.ConfigManager;
import com.tik.zbb.config.ConfigSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public final class ActionExecutor
{
    private MobActionContext mobActionContext;
    private final ActionTimers actionTimers = new ActionTimers();
    private ConfigChache configCache;

    public ActionExecutor(ConfigSnapshot configSnapshot, PathfinderMob mob)
    {
        if (!(mob.level() instanceof ServerLevel level))
            throw new IllegalStateException("The mob level is not the serverLevel.");

        mobActionContext = new MobActionContext(
                level,
                ConfigManager.getConfigSnapshot(),
                mob,
                this,
                actionTimers
        );

        reloadConfigCache(mobActionContext.level(), mobActionContext.configSnapshot().data());
    }

    public void executeBreakAction(BlockPos breakPos)
    {
        keepDataUpToDate();

        BreakAction breakAction = new BreakAction(
                breakPos,
                configCache.breakSound,
                configCache.hitSound
        );

        executeAction(breakAction, mobActionContext);
    }

    public void executeFreezeAction()
    {
        keepDataUpToDate();

        FreezeAction freezeAction = new FreezeAction();

        executeAction(freezeAction, mobActionContext);
    }

    private void executeAction(IMobAction action, MobActionContext context)
    {
        if (!action.canExecute(context)) return;

        action.execute(context);
    }


    // ================ local functions =================

    private void keepDataUpToDate()
    {
        boolean levelOutdated = mobActionContext.mob().level() instanceof ServerLevel level && mobActionContext.level() != level;
        boolean configDataOutdated = mobActionContext.configSnapshot().version() != ConfigManager.getConfigSnapshot().version();
        boolean needNewContext = levelOutdated || configDataOutdated;
        if (needNewContext)
        {
            mobActionContext = new MobActionContext(
                    (ServerLevel) mobActionContext.mob().level(),
                    ConfigManager.getConfigSnapshot(),
                    mobActionContext.mob(),
                    this,
                    actionTimers
            );
        }
        if (configDataOutdated)
        {
            reloadConfigCache(mobActionContext.level(), mobActionContext.configSnapshot().data());
        }
    }

    private void reloadConfigCache(ServerLevel level, ConfigData configData)
    {
        if (configCache == null) configCache = new ConfigChache();

        Registry<SoundEvent> soundEventRegistry = level.registryAccess().lookupOrThrow(Registries.SOUND_EVENT);
        Registry<Block> blockRegistry = level.registryAccess().lookupOrThrow(Registries.BLOCK);

        Identifier placeSoundId = Identifier.tryParse(configData.placeSoundId);
        if (placeSoundId != null)
            configCache.placeSound = soundEventRegistry.get(placeSoundId).map(Holder.Reference::value).orElse(SoundEvents.ROOTED_DIRT_PLACE);
        else configCache.placeSound = SoundEvents.ROOTED_DIRT_PLACE;

        Identifier breakSoundId = Identifier.tryParse(configData.breakSoundId);
        if (breakSoundId != null)
            configCache.breakSound = soundEventRegistry.get(breakSoundId).map(Holder.Reference::value).orElse(SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR);
        else configCache.breakSound = SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR;

        Identifier hitSoundId = Identifier.tryParse(configData.hitSoundId);
        if (hitSoundId != null)
            configCache.hitSound = soundEventRegistry.get(hitSoundId).map(Holder.Reference::value).orElse(SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR);
        else configCache.hitSound = SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR;

        Identifier blockId = Identifier.tryParse(configData.bridgeBlockId);
        if (blockId != null)
            configCache.bridgeBlock = blockRegistry.get(blockId).map(Holder.Reference::value).orElse(Blocks.DIRT);
        else configCache.bridgeBlock = Blocks.DIRT;
    }

    private class ConfigChache
    {
        public SoundEvent hitSound;
        public SoundEvent breakSound;
        public SoundEvent placeSound;
        public Block bridgeBlock;
    }
}


