package com.tik.zbb.ai.state.tactic.tactics;

import com.tik.zbb.ai.state.MobStateContext;
import com.tik.zbb.ai.state.tactic.IMobTactic;
import com.tik.zbb.utilities.HitboxScanUtility;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import static com.tik.zbb.utilities.IsFreePassUtility.isFreePass;

public class AdjustHeightToTargetTactic implements IMobTactic
{
    private static final Vec3 UP_SCAN_VEC = new Vec3(0.0, 1.0, 0.0);

    private final BlockPos.MutableBlockPos currentMobPos = new BlockPos.MutableBlockPos();
    private final BlockPos.MutableBlockPos beforeJumpMobPos = new BlockPos.MutableBlockPos();
    private State currentState = State.Idle;

    private enum State
    {
        Idle,
        Jumping,
        WaitingForBlock
    }

    @Override
    public void execute(MobStateContext context)
    {
        int mobX = Mth.floor(context.getMob().getX());
        int mobY = Mth.floor(context.getMob().getY());
        int mobZ = Mth.floor(context.getMob().getZ());
        int targetY = Mth.floor(context.getTarget().getY());

        currentMobPos.set(mobX, mobY, mobZ);

        switch (currentState)
        {
            case Idle ->
            {
                if (idle(context, targetY))
                {
                    currentState = State.Jumping;
                }
            }

            case Jumping ->
            {
                if (jumping(context, targetY))
                {
                    currentState = State.WaitingForBlock;
                }
            }

            case WaitingForBlock ->
            {
                if (waitingForBlock(context, targetY))
                {
                    currentState = State.Idle;
                }
            }
        }
    }

    @Override
    public boolean isRunning()
    {
        return currentState != State.Idle;
    }

    private boolean idle(MobStateContext context, int targetY)
    {
        if (targetY <= currentMobPos.getY() + 1)
        {
            return false;
        }

        BlockPos blockAboveUs = HitboxScanUtility.getNearestCollidingBlockWithHitbox(context.getLevel(), context.getMob(), UP_SCAN_VEC);

        return (blockAboveUs == null || isFreePass(blockAboveUs, context.getLevel())) && context.getActionExecutor().canExecuteBuildAction(currentMobPos);
    }

    private boolean jumping(MobStateContext context, int targetY)
    {
        BlockPos blockAboveUs = HitboxScanUtility.getNearestCollidingBlockWithHitbox(context.getLevel(), context.getMob(), UP_SCAN_VEC);

        if ((blockAboveUs != null && !isFreePass(blockAboveUs, context.getLevel())) || targetY <= currentMobPos.getY() + 1)
        {
            currentState = State.Idle;
            return false;
        }

        beforeJumpMobPos.set(currentMobPos.getX(), currentMobPos.getY(), currentMobPos.getZ());
        context.getMob().getJumpControl().jump();

        return true;
    }

    private boolean waitingForBlock(MobStateContext context, int targetY)
    {
        int startY = beforeJumpMobPos.getY();
        double currentY = context.getMob().getY();
        double verticalSpeed = context.getMob().getDeltaMovement().y;

        // cancel
        if (targetY <= startY + 1) return true;
        if (context.getMob().onGround()) return true;
        if (verticalSpeed < 0.0) return true;

        // wait
        if (currentY < startY + 1) return false;

        if (context.getActionExecutor().tryExecuteBuildAction(beforeJumpMobPos))
        {
            return true;
        }
        else
        {
            context.getActionExecutor().tryExecuteBreakAction(beforeJumpMobPos);
        }

        return false;
    }
}