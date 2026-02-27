package com.tik.zbb.ai.action;

import com.tik.zbb.ai.AiTimers;
import com.tik.zbb.ai.action.actions.BreakAction;
import com.tik.zbb.ai.action.actions.BuildAction;
import com.tik.zbb.ai.action.actions.FreezeAction;
import com.tik.zbb.ai.action.actions.GoToTargetAction;
import com.tik.zbb.config.ConfigData;
import com.tik.zbb.config.ConfigManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public final class ActionExecutor
{
    private final BreakAction breakAction = new BreakAction();
    private final BuildAction buildAction = new BuildAction();
    private final FreezeAction freezeAction = new FreezeAction();
    private final GoToTargetAction goToTargetAction = new GoToTargetAction();

    private MobActionContext mobActionContext;
    private ConfigCache configCache;

    public ActionExecutor(PathfinderMob mob, AiTimers aiTimers)
    {
        if (!(mob.level() instanceof ServerLevel level))
            throw new IllegalStateException("The mob level is not the serverLevel.");

        mobActionContext = new MobActionContext(
                level,
                ConfigManager.getConfigSnapshot(),
                mob,
                this,
                aiTimers
        );

        reloadConfigCache(mobActionContext.level(), mobActionContext.configSnapshot().data());
    }

    public boolean tryExecuteBreakAction(BlockPos breakPos)
    {
        keepDataUpToDate();
        breakAction.setup(breakPos, configCache.breakSound, configCache.hitSound);

        return tryExecuteAction(breakAction);
    }

    public boolean tryExecuteBuildAction(BlockPos buildPos)
    {
        keepDataUpToDate();
        buildAction.setup(buildPos, configCache.bridgeBlock, configCache.placeSound);

        return tryExecuteAction(buildAction);
    }

    public boolean tryExecuteFreezeAction()
    {
        keepDataUpToDate();

        return tryExecuteAction(freezeAction);
    }

    public boolean tryExecuteGoToTargetAction(LivingEntity target)
    {
        keepDataUpToDate();
        goToTargetAction.setup(mobActionContext.mob(), target);

        return tryExecuteAction(goToTargetAction);
    }

    private boolean tryExecuteAction(IMobAction action)
    {
        if (!action.canExecute(mobActionContext)) return false;

        action.execute(mobActionContext);
        return true;
    }


    // =================================

    private void keepDataUpToDate()
    {
        if (!(mobActionContext.mob().level() instanceof ServerLevel level))
            throw new IllegalStateException("The mob level is not the serverLevel.");

        boolean levelOutdated = mobActionContext.level() != level;
        boolean configDataOutdated = mobActionContext.configSnapshot().version() != ConfigManager.getConfigSnapshot().version();
        boolean needNewContext = levelOutdated || configDataOutdated;
        if (needNewContext)
        {
            mobActionContext = new MobActionContext(
                    level,
                    ConfigManager.getConfigSnapshot(),
                    mobActionContext.mob(),
                    this,
                    mobActionContext.aiTimers()
            );
        }
        if (configDataOutdated)
        {
            reloadConfigCache(mobActionContext.level(), mobActionContext.configSnapshot().data());
        }
    }

    private void reloadConfigCache(ServerLevel level, ConfigData configData)
    {
        if (configCache == null) configCache = new ConfigCache();

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

    private class ConfigCache
    {
        public SoundEvent hitSound;
        public SoundEvent breakSound;
        public SoundEvent placeSound;
        public Block bridgeBlock;
    }
}


