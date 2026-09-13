package com.tik.zbb.ai.state.tactic.tactics;

import com.tik.zbb.ai.state.MobStateContext;
import com.tik.zbb.ai.state.tactic.IMobTactic;
import com.tik.zbb.utilities.HitboxScanUtility;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import static com.tik.zbb.utilities.IsFreePassUtility.isFreePass;

public class AdjustHeightToTargetTactic implements IMobTactic
{
    private static final Vec3 UP_SCAN_VEC = new Vec3(0.0, 1.0, 0.0);

    private final BlockPos.MutableBlockPos currentMobPos = new BlockPos.MutableBlockPos();
    private final BlockPos.MutableBlockPos beforeJumpMobPos = new BlockPos.MutableBlockPos();
    private State currentState;

    private enum State
    {
        Idle,
        Jumping,
        WaitingForBlock
    }

    public AdjustHeightToTargetTactic()
    {
        resetTransientState();
    }

    @Override
    public void execute(MobStateContext context)
    {
        if (!context.getConfigSnapshot().game().ai().tactics().adjustHeightToTarget())
        {
            resetTransientState();
            return;
        }

        int mobX = Mth.floor(context.getMob().getX());
        int mobY = Mth.floor(context.getMob().getY());
        int mobZ = Mth.floor(context.getMob().getZ());
        int targetY = Mth.floor(context.getTarget().getY());

        currentMobPos.set(mobX, mobY, mobZ);

        for (int i = 0; i < 2; i++)
        {
            State prevState = currentState;

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
                    if (jumping(context))
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

            if (currentState == State.WaitingForBlock || currentState == prevState)
            {
                break;
            }
        }
    }

    @Override
    public boolean isRunning()
    {
        return currentState != State.Idle;
    }

    @Override
    public void resetTransientState()
    {
        currentState = State.Idle;
    }

    private boolean idle(MobStateContext context, int targetY)
    {
        if (targetY <= currentMobPos.getY() + 1) return false;

        BlockPos blockAboveUs = HitboxScanUtility.getNearestCollidingBlockWithHitbox(context.getLevel(), context.getMob(), UP_SCAN_VEC);
        BlockPos posUnderBottomCenter = getBlockUnderBottomCenter(context);
        BlockPos aboveBottomCenter = posUnderBottomCenter.above();

        if (blockAboveUs != null && !isFreePass(blockAboveUs, context.getLevel()))
        {
            context.getActionExecutor().tryExecuteBreakAction(blockAboveUs);
            return false;
        }
        if (!context.getActionExecutor().canExecuteBuildAction(aboveBottomCenter))
        {
            context.getActionExecutor().tryExecuteBreakAction(aboveBottomCenter);
            return false;
        }

        return true;
    }

    private boolean jumping(MobStateContext context)
    {
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

        BlockPos posUnderBottomCenter = getBlockUnderBottomCenter(context);

        if (context.getActionExecutor().tryExecuteBuildAction(posUnderBottomCenter))
        {
            context.getActionExecutor().tryExecuteFreezeAction();
            return true;
        }
        else
        {
            if (context.getActionExecutor().tryExecuteBreakAction(posUnderBottomCenter))
            {
                context.getActionExecutor().tryExecuteFreezeAction();
            }
            return true;
        }
    }

    private BlockPos getBlockUnderBottomCenter(MobStateContext context)
    {
        AABB box = context.getMob().getBoundingBox();

        double centerX = (box.minX + box.maxX) * 0.5D;
        double bottomY = box.minY;
        double centerZ = (box.minZ + box.maxZ) * 0.5D;

        int x = Mth.floor(centerX);
        int y = Mth.floor(bottomY - 1);
        int z = Mth.floor(centerZ);

        return new BlockPos(x, y, z);
    }
}
