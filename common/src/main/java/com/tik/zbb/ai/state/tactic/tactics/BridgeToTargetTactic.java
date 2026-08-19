package com.tik.zbb.ai.state.tactic.tactics;

import com.tik.zbb.ai.state.MobStateContext;
import com.tik.zbb.ai.state.tactic.IMobTactic;
import com.tik.zbb.utilities.HitboxScanUtility;
import com.tik.zbb.utilities.IsFreePassUtility;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BridgeToTargetTactic implements IMobTactic
{
    private static final Vec3 DOWN_SCAN_VEC = new Vec3(0.0, -1.0, 0.0);
    private static final double MIN_HORIZONTAL_DIRECTION_SQ = 1.0E-12D;
    private static final double AXIS_EPSILON = 1.0E-12D;
    private static final double CROSSING_TIME_EPSILON = 1.0E-9D;
    private static final Comparator<BridgeCandidate> CANDIDATE_COMPARATOR = Comparator.comparingDouble(BridgeCandidate::centerDistanceSq);

    private final BlockPos.MutableBlockPos belowMobPos = new BlockPos.MutableBlockPos();
    private final List<BridgeCandidate> bridgeCandidates = new ArrayList<>();

    @Override
    public void execute(MobStateContext context)
    {
        if (!context.getConfigSnapshot().game().ai().tactics().bridgeToTarget()) return;

        Mob mob = context.getMob();
        AABB box = mob.getBoundingBox();

        int mobY = Mth.floor(box.minY);
        int targetY = Mth.floor(context.getTarget().getY());

        double mobCenterX = (box.minX + box.maxX) * 0.5D;
        double mobCenterZ = (box.minZ + box.maxZ) * 0.5D;
        belowMobPos.set(Mth.floor(mobCenterX), mobY - 1, Mth.floor(mobCenterZ));

        boolean belowUsEmpty = HitboxScanUtility.getNearestCollidingBlockWithHitbox(context.getLevel(), mob, DOWN_SCAN_VEC) == null;

        if (belowUsEmpty && targetY >= mobY)
        {
            if (context.getActionExecutor().tryExecuteBuildAction(belowMobPos))
            {
                context.getActionExecutor().tryExecuteFreezeAction();
                return;
            }
        }

        collectNextBridgeCandidates(mob, context.getTarget());

        for (int i = 0, size = bridgeCandidates.size(); i < size; i++)
        {
            BridgeCandidate candidate = bridgeCandidates.get(i);
            BlockPos belowFrontPos = candidate.supportPos();
            BlockPos frontBlockPos = belowFrontPos.above();
            BlockPos twoBelowFrontPos = belowFrontPos.below();

            boolean frontEmpty = IsFreePassUtility.isFreePass(frontBlockPos, context.getLevel());
            boolean belowFrontEmpty = IsFreePassUtility.isFreePass(belowFrontPos, context.getLevel());
            boolean below2FrontEmpty = IsFreePassUtility.isFreePass(twoBelowFrontPos, context.getLevel());

            if (!frontEmpty || !belowFrontEmpty || !below2FrontEmpty) continue;

            if (context.getActionExecutor().tryExecuteBuildAction(belowFrontPos))
            {
                context.getActionExecutor().tryExecuteFreezeAction();
                return;
            }

            if (context.getActionExecutor().tryExecuteBreakAction(belowFrontPos))
            {
                context.getActionExecutor().tryExecuteFreezeAction();
                return;
            }
        }
    }

    private void collectNextBridgeCandidates(Mob mob, LivingEntity target)
    {
        bridgeCandidates.clear();

        AABB box = mob.getBoundingBox();
        AABB targetBox = target.getBoundingBox();

        double mobCenterX = (box.minX + box.maxX) * 0.5D;
        double mobCenterZ = (box.minZ + box.maxZ) * 0.5D;
        double targetCenterX = (targetBox.minX + targetBox.maxX) * 0.5D;
        double targetCenterZ = (targetBox.minZ + targetBox.maxZ) * 0.5D;

        double dx = targetCenterX - mobCenterX;
        double dz = targetCenterZ - mobCenterZ;
        double lengthSq = dx * dx + dz * dz;

        if (lengthSq < MIN_HORIZONTAL_DIRECTION_SQ) return;

        double inverseLength = 1.0D / Math.sqrt(lengthSq);
        dx *= inverseLength;
        dz *= inverseLength;

        AxisCrossing xCrossing = getNextXCrossing(box, dx);
        AxisCrossing zCrossing = getNextZCrossing(box, dz);
        double firstCrossingTime = Math.min(xCrossing.time(), zCrossing.time());

        if (!Double.isFinite(firstCrossingTime)) return;

        int supportY = Mth.floor(box.minY) - 1;
        AABB crossingBox = box.move(dx * firstCrossingTime, 0.0D, dz * firstCrossingTime);
        double crossingCenterX = (crossingBox.minX + crossingBox.maxX) * 0.5D;
        double crossingCenterZ = (crossingBox.minZ + crossingBox.maxZ) * 0.5D;

        if (sameCrossingTime(xCrossing.time(), firstCrossingTime))
        {
            int minZ = firstOccupiedBlock(crossingBox.minZ);
            int maxZ = lastOccupiedBlock(crossingBox.maxZ);

            for (int z = minZ; z <= maxZ; z++)
            {
                addCandidate(xCrossing.newBlockCoordinate(), supportY, z, crossingCenterX, crossingCenterZ);
            }
        }

        if (sameCrossingTime(zCrossing.time(), firstCrossingTime))
        {
            int minX = firstOccupiedBlock(crossingBox.minX);
            int maxX = lastOccupiedBlock(crossingBox.maxX);

            for (int x = minX; x <= maxX; x++)
            {
                addCandidate(x, supportY, zCrossing.newBlockCoordinate(), crossingCenterX, crossingCenterZ);
            }
        }

        bridgeCandidates.sort(CANDIDATE_COMPARATOR);
    }

    private AxisCrossing getNextXCrossing(AABB box, double dx)
    {
        if (dx > AXIS_EPSILON)
        {
            int currentRightmostBlock = Mth.floor(Math.nextDown(box.maxX));
            int newBlockX = currentRightmostBlock + 1;
            double time = Math.max(0.0D, (newBlockX - box.maxX) / dx);
            return new AxisCrossing(time, newBlockX);
        }

        if (dx < -AXIS_EPSILON)
        {
            int currentLeftmostBlock = Mth.floor(box.minX);
            int newBlockX = currentLeftmostBlock - 1;
            double boundaryX = currentLeftmostBlock;
            double time = Math.max(0.0D, (box.minX - boundaryX) / -dx);
            return new AxisCrossing(time, newBlockX);
        }

        return AxisCrossing.NEVER;
    }

    private AxisCrossing getNextZCrossing(AABB box, double dz)
    {
        if (dz > AXIS_EPSILON)
        {
            int currentFrontmostBlock = Mth.floor(Math.nextDown(box.maxZ));
            int newBlockZ = currentFrontmostBlock + 1;
            double time = Math.max(0.0D, (newBlockZ - box.maxZ) / dz);
            return new AxisCrossing(time, newBlockZ);
        }

        if (dz < -AXIS_EPSILON)
        {
            int currentBackmostBlock = Mth.floor(box.minZ);
            int newBlockZ = currentBackmostBlock - 1;
            double boundaryZ = currentBackmostBlock;
            double time = Math.max(0.0D, (box.minZ - boundaryZ) / -dz);
            return new AxisCrossing(time, newBlockZ);
        }

        return AxisCrossing.NEVER;
    }

    private void addCandidate(int x, int y, int z, double crossingCenterX, double crossingCenterZ)
    {
        for (int i = 0, size = bridgeCandidates.size(); i < size; i++)
        {
            BlockPos existingPos = bridgeCandidates.get(i).supportPos();
            if (existingPos.getX() == x && existingPos.getY() == y && existingPos.getZ() == z)
            {
                return;
            }
        }

        BlockPos pos = new BlockPos(x, y, z);
        double blockCenterX = x + 0.5D;
        double blockCenterZ = z + 0.5D;
        double ddx = blockCenterX - crossingCenterX;
        double ddz = blockCenterZ - crossingCenterZ;

        bridgeCandidates.add(new BridgeCandidate(pos, ddx * ddx + ddz * ddz));
    }

    private int firstOccupiedBlock(double min)
    {
        return Mth.floor(min);
    }

    private int lastOccupiedBlock(double max)
    {
        return Mth.floor(Math.nextDown(max));
    }

    private boolean sameCrossingTime(double a, double b)
    {
        if (!Double.isFinite(a) || !Double.isFinite(b))
        {
            return a == b;
        }

        double scale = Math.max(1.0D, Math.max(Math.abs(a), Math.abs(b)));
        return Math.abs(a - b) <= CROSSING_TIME_EPSILON * scale;
    }

    private record AxisCrossing(double time, int newBlockCoordinate)
    {
        private static final AxisCrossing NEVER = new AxisCrossing(Double.POSITIVE_INFINITY, 0);
    }

    private record BridgeCandidate(BlockPos supportPos, double centerDistanceSq) {}
}
