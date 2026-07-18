package com.tik.zbb.ai.state.tactic.tactics;

import com.tik.zbb.ai.state.MobStateContext;
import com.tik.zbb.ai.state.tactic.IMobTactic;
import com.tik.zbb.utilities.HitboxScanUtility;
import com.tik.zbb.utilities.IsFreePassUtility;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

public class BridgeToTargetTactic implements IMobTactic
{
    private final Vec3 DOWN_SCAN_VEC = new Vec3(0.0, -1.0, 0.0);

    private final BlockPos.MutableBlockPos frontBlockPos = new BlockPos.MutableBlockPos();
    private final BlockPos.MutableBlockPos belowMobPos = new BlockPos.MutableBlockPos();
    private final BlockPos.MutableBlockPos belowFrontPos = new BlockPos.MutableBlockPos();
    private final BlockPos.MutableBlockPos twoBelowFrontPos = new BlockPos.MutableBlockPos();

    @Override
    public void execute(MobStateContext context)
    {
        if (!context.getConfigSnapshot().game().ai().tactics().bridgeToTarget()) return;

        int mobX = Mth.floor(context.getMob().getX());
        int mobY = Mth.floor(context.getMob().getY());
        int mobZ = Mth.floor(context.getMob().getZ());
        int targetY = Mth.floor(context.getTarget().getY());

        updateFrontBlock(context.getMob(), context.getTarget());
        belowMobPos.set(mobX, mobY - 1, mobZ);
        belowFrontPos.set(frontBlockPos.getX(), frontBlockPos.getY() - 1, frontBlockPos.getZ());
        twoBelowFrontPos.set(frontBlockPos.getX(), frontBlockPos.getY() - 2, frontBlockPos.getZ());

        boolean belowUsEmpty = HitboxScanUtility.getNearestCollidingBlockWithHitbox(context.getLevel(), context.getMob(), DOWN_SCAN_VEC) == null;
        if (belowUsEmpty && targetY >= mobY)
        {
            if (context.getActionExecutor().tryExecuteBuildAction(belowMobPos))
            {
                context.getActionExecutor().tryExecuteFreezeAction();
            }
        }

        boolean frontEmpty = IsFreePassUtility.isFreePass(frontBlockPos, context.getLevel());
        boolean belowFrontEmpty = IsFreePassUtility.isFreePass(belowFrontPos, context.getLevel());
        boolean below2FrontEmpty = IsFreePassUtility.isFreePass(twoBelowFrontPos, context.getLevel());
        if (frontEmpty && belowFrontEmpty && below2FrontEmpty)
        {
            if (context.getActionExecutor().tryExecuteBuildAction(belowFrontPos))
            {
                context.getActionExecutor().tryExecuteFreezeAction();
            }
            else
            {
                if (context.getActionExecutor().tryExecuteBreakAction(belowFrontPos))
                {
                    context.getActionExecutor().tryExecuteFreezeAction();
                }
            }
        }
    }

    public void updateFrontBlock(Mob mob, LivingEntity target)
    {
        var box = mob.getBoundingBox();

        double dx = target.getX() - mob.getX();
        double dz = target.getZ() - mob.getZ();

        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 1e-6)
        {
            frontBlockPos.set(mob.getX(), mob.getY(), mob.getZ());
            return;
        }

        dx /= len;
        dz /= len;

        double frontX = dx > 0 ? box.maxX : box.minX;
        double frontZ = dz > 0 ? box.maxZ : box.minZ;

        frontX += dx;
        frontZ += dz;

        int blockX = Mth.floor(frontX);
        int blockY = Mth.floor(box.minY);
        int blockZ = Mth.floor(frontZ);

        frontBlockPos.set(blockX, blockY, blockZ);
    }
}
