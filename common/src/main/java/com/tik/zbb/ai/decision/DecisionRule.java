package com.tik.zbb.ai.decision;

import com.tik.zbb.ai.context.AIContext;

public class DecisionRule
{
    int priority();

    Optional<MobAction> decide(AIContext ctx);
}
