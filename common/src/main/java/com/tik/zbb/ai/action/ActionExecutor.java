package com.tik.zbb.ai.action;

import com.tik.zbb.ai.AiTimers;
import com.tik.zbb.ai.action.actions.breakk.BreakAction;
import com.tik.zbb.ai.action.actions.breakk.BreakRequest;
import com.tik.zbb.ai.action.actions.build.BuildAction;
import com.tik.zbb.ai.action.actions.build.BuildRequest;
import com.tik.zbb.ai.action.actions.freeze.FreezeAction;
import com.tik.zbb.ai.action.actions.freeze.FreezeRequest;
import com.tik.zbb.config.ConfigData;
import com.tik.zbb.config.ConfigManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public final class ActionExecutor
{
    private final BreakAction breakAction = new BreakAction();
    private final BuildAction buildAction = new BuildAction();
    private final FreezeAction freezeAction = new FreezeAction();

    private MobActionContext mobActionContext;
    private ConfigCache configCache;

    public ActionExecutor(PathfinderMob mob, AiTimers aiTimers)
    {
        if (!(mob.level() instanceof ServerLevel level))
            throw new IllegalStateException("The mob level is not the serverLevel.");

        Registry<EntityType> entityTypeRegistry = level.registryAccess().registryOrThrow(Registries.ENTITY_TYPE);

        mobActionContext = new MobActionContext(
                level,
                ConfigManager.getConfigSnapshot(),
                mob,
                aiTimers,
                entityTypeRegistry.getKey(mob.getType())
        );

        reloadConfigCache(mobActionContext.level(), mobActionContext.configSnapshot().data());
    }

    public boolean canExecuteBreakAction(BlockPos breakPos)
    {
        keepDataUpToDate();
        return canExecuteAction(breakAction, new BreakRequest(breakPos));
    }

    public boolean canExecuteBuildAction(BlockPos buildPos)
    {
        keepDataUpToDate();
        return canExecuteAction(buildAction, new BuildRequest(buildPos, configCache.bridgeBlock));
    }

    public boolean canExecuteFreezeAction()
    {
        keepDataUpToDate();
        return canExecuteAction(freezeAction, new FreezeRequest());
    }

    public boolean tryExecuteBreakAction(BlockPos breakPos)
    {
        keepDataUpToDate();
        return tryExecuteAction(breakAction, new BreakRequest(breakPos));
    }

    public boolean tryExecuteBuildAction(BlockPos buildPos)
    {
        keepDataUpToDate();
        return tryExecuteAction(buildAction, new BuildRequest(buildPos, configCache.bridgeBlock));
    }

    public boolean tryExecuteFreezeAction()
    {
        keepDataUpToDate();
        return tryExecuteAction(freezeAction, new FreezeRequest());
    }

    private <R> boolean tryExecuteAction(IMobAction<R> action, R request)
    {
        if (!action.canExecute(mobActionContext, request)) return false;

        action.execute(mobActionContext, request);
        return true;
    }

    private <R> boolean canExecuteAction(IMobAction<R> action, R request)
    {
        return action.canExecute(mobActionContext, request);
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

        Registry<Block> blockRegistry = level.registryAccess().registryOrThrow(Registries.BLOCK);

        Block bridgeBlock = blockRegistry.get(ResourceLocation.tryParse(configData.blocks.bridgeBlockId));
        configCache.bridgeBlock = bridgeBlock != null ? bridgeBlock : Blocks.DIRT;
    }

    private class ConfigCache
    {
        public Block bridgeBlock;
    }
}


