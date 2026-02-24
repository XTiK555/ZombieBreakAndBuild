package com.tik.zbb.ai.decision;

import java.util.Comparator;
import java.util.List;

public class DecisionPlanner
{
    private final List<DecisionRule> rules;

    public DecisionPlanner(List<DecisionRule> rules)
    {
        this.rules = rules.stream().sorted(Comparator.comparingInt(DecisionRule::priority)).toList();
    }

    public MobAction plan(AIContext ctx)
    {
        for (var r : rules)
        {
            var a = r.decide(ctx);
            if (a.isPresent()) return a.get();
        }
        return NoOpAction.INSTANCE;
    }
}
