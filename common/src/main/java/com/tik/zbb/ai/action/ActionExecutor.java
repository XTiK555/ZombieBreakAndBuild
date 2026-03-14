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
import net.minecraft.world.entity.EntityType;
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

        Registry<EntityType> entityTypeRegistry = level.registryAccess().lookupOrThrow(Registries.ENTITY_TYPE);

        mobActionContext = new MobActionContext(
                level,
                ConfigManager.getConfigSnapshot(),
                mob,
                this,
                aiTimers,
                entityTypeRegistry.getKey(mob.getType())
        );

        reloadConfigCache(mobActionContext.level(), mobActionContext.configSnapshot().data());
    }

    public boolean tryExecuteBreakAction(BlockPos breakPos)
    {
        keepDataUpToDate();
        breakAction.setup(breakPos);

        return tryExecuteAction(breakAction);
    }

    public boolean tryExecuteBuildAction(BlockPos buildPos)
    {
        keepDataUpToDate();
        buildAction.setup(buildPos, configCache.bridgeBlock);

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
                    mobActionContext.aiTimers(),
                    mobActionContext.mobId()
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

        Registry<Block> blockRegistry = level.registryAccess().lookupOrThrow(Registries.BLOCK);

        Identifier blockId = Identifier.tryParse(configData.blocks.bridgeBlockId);
        if (blockId != null)
            configCache.bridgeBlock = blockRegistry.get(blockId).map(Holder.Reference::value).orElse(Blocks.DIRT);
        else configCache.bridgeBlock = Blocks.DIRT;
    }

    private class ConfigCache
    {
        public Block bridgeBlock;
    }
}


