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
    private BlockPos.MutableBlockPos tmpBlockPos = new BlockPos.MutableBlockPos();
    private BlockPos.MutableBlockPos frontBlockPos = new BlockPos.MutableBlockPos();

    @Override
    public void execute(MobStateContext context)
    {
        int mobX = Mth.floor(context.getMob().getX());
        int mobY = Mth.floor(context.getMob().getY());
        int mobZ = Mth.floor(context.getMob().getZ());
        int targetY = Mth.floor(context.getTarget().getY());

        boolean belowUsEmpty = HitboxScanUtility.getNearestCollidingBlock(context.getLevel(), context.getMob(), new Vec3(0, -1, 0)) == null;
        if (belowUsEmpty && targetY >= context.getMob().getY())
        {
            context.getActionExecutor().tryExecuteBuildAction(tmpBlockPos.set(mobX, mobY - 1, mobZ));
        }

        frontBlockPos = getFrontBlock(context.getMob(), context.getTarget()).mutable();

        boolean frontEmpty = IsFreePassUtility.isFreePass(frontBlockPos, context.getLevel());
        boolean belowFrontEmpty = IsFreePassUtility.isFreePass(tmpBlockPos.set(frontBlockPos.getX(), frontBlockPos.getY() - 1, frontBlockPos.getZ()), context.getLevel());
        boolean below2FrontEmpty = IsFreePassUtility.isFreePass(tmpBlockPos.set(frontBlockPos.getX(), frontBlockPos.getY() - 2, frontBlockPos.getZ()), context.getLevel());
        if (frontEmpty && belowFrontEmpty && below2FrontEmpty)
        {
            context.getActionExecutor().tryExecuteBuildAction(tmpBlockPos.set(frontBlockPos.getX(), frontBlockPos.getY() - 1, frontBlockPos.getZ()));
        }
    }

    public static BlockPos getFrontBlock(Mob mob, LivingEntity target)
    {
        var box = mob.getBoundingBox();

        double dx = target.getX() - mob.getX();
        double dz = target.getZ() - mob.getZ();

        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 1e-6)
            return mob.blockPosition();

        dx /= len;
        dz /= len;

        double frontX = dx > 0 ? box.maxX : box.minX;
        double frontZ = dz > 0 ? box.maxZ : box.minZ;

        frontX += dx;
        frontZ += dz;

        int blockX = Mth.floor(frontX);
        int blockY = Mth.floor(box.minY);
        int blockZ = Mth.floor(frontZ);

        return new BlockPos(blockX, blockY, blockZ);
    }
}
