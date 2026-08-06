package com.tik.zbb.utilities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TargetVisibilityThroughBlocksUtilityTest
{
    @Test
    void reachedAxisIsNotCrossedAgainAtNegativeBoundaryTie()
    {
        int crossed = TargetVisibilityThroughBlocksUtility.crossedAxes(
                -2, -2, -2,
                -2, -1, -2,
                Double.POSITIVE_INFINITY, 1.0D, 1.0D,
                1.0D
        );

        assertEquals(TargetVisibilityThroughBlocksUtility.AXIS_Y, crossed);
    }
}
