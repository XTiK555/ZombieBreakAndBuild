package com.tik.zbb.ai.action;

import com.tik.zbb.ai.AiTimers;
import com.tik.zbb.ai.action.actions.breakk.BreakAction;
import com.tik.zbb.ai.action.actions.breakk.BreakRequest;
import com.tik.zbb.ai.action.actions.build.BuildAction;
import com.tik.zbb.ai.action.actions.build.BuildRequest;
import com.tik.zbb.ai.action.actions.freeze.FreezeAction;
import com.tik.zbb.ai.action.actions.freeze.FreezeRequest;
import com.tik.zbb.config.ConfigManager;
import com.tik.zbb.config.ConfigSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.block.Block;

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

        Registry<EntityType> entityTypeRegistry = level.registryAccess().lookupOrThrow(Registries.ENTITY_TYPE);

        mobActionContext = new MobActionContext(
                level,
                ConfigManager.getConfigSnapshot(),
                mob,
                aiTimers,
                entityTypeRegistry.getKey(mob.getType())
        );

        reloadConfigCache(mobActionContext.level(), mobActionContext.configSnapshot());
    }

    public void tick()
    {
        keepDataUpToDate();
    }

    public boolean canExecuteBreakAction(BlockPos breakPos)
    {
        return canExecuteAction(breakAction, new BreakRequest(breakPos));
    }

    public boolean canExecuteBuildAction(BlockPos buildPos)
    {
        return canExecuteAction(buildAction, new BuildRequest(buildPos, configCache.bridgeBlock));
    }

    public boolean canExecuteFreezeAction()
    {
        return canExecuteAction(freezeAction, new FreezeRequest());
    }

    public boolean tryExecuteBreakAction(BlockPos breakPos)
    {
        return tryExecuteAction(breakAction, new BreakRequest(breakPos));
    }

    public boolean tryExecuteBuildAction(BlockPos buildPos)
    {
        return tryExecuteAction(buildAction, new BuildRequest(buildPos, configCache.bridgeBlock));
    }

    public boolean tryExecuteFreezeAction()
    {
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

        ConfigSnapshot configSnapshot = ConfigManager.getConfigSnapshot();

        boolean levelOutdated = mobActionContext.level() != level;
        boolean configDataOutdated = mobActionContext.configSnapshot().version() != configSnapshot.version();
        boolean needNewContext = levelOutdated || configDataOutdated;
        if (needNewContext)
        {
            mobActionContext = new MobActionContext(
                    level,
                    configSnapshot,
                    mobActionContext.mob(),
                    mobActionContext.aiTimers(),
                    mobActionContext.mobId()
            );
        }
        if (levelOutdated || configDataOutdated)
        {
            reloadConfigCache(mobActionContext.level(), mobActionContext.configSnapshot());
        }
    }

    private void reloadConfigCache(ServerLevel level, ConfigSnapshot configSnapshot)
    {
        if (configCache == null) configCache = new ConfigCache();

        configCache.bridgeBlock = selectBridgeBlock(level, configSnapshot);
    }

    private Block selectBridgeBlock(ServerLevel level, ConfigSnapshot configSnapshot)
    {
        Block mobBlock = mobActionContext.mobId() == null
                ? null
                : configSnapshot.game().blocks().mobPlaceBlockOverrideMap().get(mobActionContext.mobId());
        if (mobBlock != null)
        {
            return mobBlock;
        }

        Block dimensionBlock = configSnapshot.game().blocks().dimensionPlaceBlockMap().get(level.dimension().identifier());
        if (dimensionBlock != null)
        {
            return dimensionBlock;
        }

        return configSnapshot.game().blocks().fallbackPlaceBlock();
    }

    private class ConfigCache
    {
        public Block bridgeBlock;
    }
}
