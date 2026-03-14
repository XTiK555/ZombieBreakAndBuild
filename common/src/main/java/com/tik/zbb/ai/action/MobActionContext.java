package com.tik.zbb.ai.action;

import com.tik.zbb.ai.AiTimers;
import com.tik.zbb.config.ConfigSnapshot;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;

public record MobActionContext(
        ServerLevel level,
        ConfigSnapshot configSnapshot,
        PathfinderMob mob,
        ActionExecutor executor,
        AiTimers aiTimers,
        Identifier mobId
) {}
