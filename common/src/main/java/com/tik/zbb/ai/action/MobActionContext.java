package com.tik.zbb.ai.action;

import com.tik.zbb.config.ConfigSnapshot;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;

public record MobActionContext(
        ServerLevel level,
        ConfigSnapshot configSnapshot,
        PathfinderMob mob,
        ActionExecutor executor,
        ActionTimers actionTimers
) {}
