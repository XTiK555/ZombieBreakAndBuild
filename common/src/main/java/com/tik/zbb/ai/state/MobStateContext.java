package com.tik.zbb.ai.state;

import com.tik.zbb.ai.AiTimers;
import com.tik.zbb.ai.action.ActionExecutor;
import com.tik.zbb.config.ConfigSnapshot;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;

public class MobStateContext
{
    private final ActionExecutor actionExecutor;
    private final PathfinderMob mob;
    private ServerLevel level;
    private final AiTimers aiTimers;
    private ConfigSnapshot configSnapshot;
    private LivingEntity target;

    public MobStateContext(ActionExecutor actionExecutor, PathfinderMob mob, ServerLevel level, AiTimers aiTimers, ConfigSnapshot configSnapshot)
    {
        this.actionExecutor = actionExecutor;
        this.mob = mob;
        this.level = level;
        this.aiTimers = aiTimers;
        this.configSnapshot = configSnapshot;
    }

    public ActionExecutor getActionExecutor() {return actionExecutor;}

    public PathfinderMob getMob() {return mob;}

    public ServerLevel getLevel() {return level;}

    public AiTimers getAiTimers() {return aiTimers;}

    public ConfigSnapshot getConfigSnapshot() {return configSnapshot;}

    public LivingEntity getTarget() {return target;}


    public void setConfigSnapshot(ConfigSnapshot configSnapshot) {this.configSnapshot = configSnapshot;}

    public void setLevel(ServerLevel level) {this.level = level;}

    public void setTarget(LivingEntity target) {this.target = target;}
}
