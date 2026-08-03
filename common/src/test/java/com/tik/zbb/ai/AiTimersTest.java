package com.tik.zbb.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiTimersTest
{
    @Test
    void cooldownsPassAtTheirExactDeadline()
    {
        AiTimers timers = new AiTimers();
        timers.setBreakCooldownUntil(10);
        timers.setBuildCooldownUntil(20);
        timers.setMitigateDangerousBlocksCooldownUntil(30);

        assertAll(
                () -> assertFalse(timers.breakCooldownPassed(9)),
                () -> assertTrue(timers.breakCooldownPassed(10)),
                () -> assertFalse(timers.buildCooldownPassed(19)),
                () -> assertTrue(timers.buildCooldownPassed(20)),
                () -> assertFalse(timers.mitigateDangerousBlocksCooldownPassed(29)),
                () -> assertTrue(timers.mitigateDangerousBlocksCooldownPassed(30))
        );
    }

    @Test
    void cooldownsAreInitiallyAvailableAndIndependent()
    {
        AiTimers timers = new AiTimers();
        timers.setBreakCooldownUntil(Long.MAX_VALUE);

        assertAll(
                () -> assertFalse(timers.breakCooldownPassed(0)),
                () -> assertTrue(timers.buildCooldownPassed(0)),
                () -> assertTrue(timers.mitigateDangerousBlocksCooldownPassed(0))
        );
    }
}
