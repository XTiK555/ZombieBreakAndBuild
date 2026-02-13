package com.tik.zbb.goals;

import com.tik.zbb.BlockStorage;
import com.tik.zbb.Config;
import com.tik.zbb.utilities.SecondsToTicksUtility;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraftforge.registries.ForgeRegistries;

public class BreakAndBuildGoal extends Goal
{
    private final PathfinderMob mob;
    private final Level level;
    private final Block bridgeBlock;
    private final SoundEvent placeSound, hitSound, breakSound;

    private long lastBuildTick = Long.MIN_VALUE;
    private long lastBreakTick = Long.MIN_VALUE;
    private long freezeUntilTick = Long.MIN_VALUE;
    private long nextPathCheckTick = Long.MIN_VALUE;
    private long nextSearchDangerousTick = Long.MIN_VALUE;
    private long nextGoToTargetTick = Long.MIN_VALUE;

    private int stuckTicks = 0;
    private double lastDistSq = Double.NaN;
    private boolean isBreakAndBuild = false;

    // carried out from tick() for optimization
    private final BlockPos.MutableBlockPos tickTmpBlockPos = new BlockPos.MutableBlockPos();
    private final BlockPos.MutableBlockPos tickFrontBlock = new BlockPos.MutableBlockPos();

    // carried out from mitigateNearbyDanger for optimization
    BlockPos.MutableBlockPos mndBlockPos = new BlockPos.MutableBlockPos();
    BlockPos.MutableBlockPos mndCover = new BlockPos.MutableBlockPos();

    public BreakAndBuildGoal(PathfinderMob mob)
    {
        this.mob = mob;
        this.level = mob.level();

        Block block = ForgeRegistries.BLOCKS.getValue(Identifier.tryParse(Config.BRIDGE_BLOCK_ID.get()));
        this.bridgeBlock = block != null ? block : Blocks.GRAVEL;

        SoundEvent sound1 = ForgeRegistries.SOUND_EVENTS.getValue(Identifier.tryParse(Config.PLACE_SOUND_ID.get()));
        this.placeSound = sound1 != null ? sound1 : SoundEvents.GRAVEL_PLACE;

        SoundEvent sound2 = ForgeRegistries.SOUND_EVENTS.getValue(Identifier.tryParse(Config.HIT_SOUND_ID.get()));
        this.hitSound = sound2 != null ? sound2 : SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR;

        SoundEvent sound3 = ForgeRegistries.SOUND_EVENTS.getValue(Identifier.tryParse(Config.BREAK_SOUND_ID.get()));
        this.breakSound = sound3 != null ? sound3 : SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR;
    }

    @Override
    public void tick()
    {
        LivingEntity target = mob.getTarget();
        if (target == null) return;

        final long currentTime = level.getGameTime();

        updateStuckState(target, currentTime);

        if (currentTime >= nextSearchDangerousTick)
        {
            mitigateNearbyDanger();
            nextSearchDangerousTick = level.getGameTime() + SecondsToTicksUtility.toTicks(Config.SEARCH_DANGEROUS_INTERVAL.get(), 1);
        }

        if (currentTime >= nextPathCheckTick)
        {
            checkPath();
            nextPathCheckTick = level.getGameTime() + SecondsToTicksUtility.toTicks(Config.PATH_CHECK_INTERVAL.get(), 1);
        }

        if (isBreakAndBuild)
        {
            handleVerticalActions(target);
            handleBridgeGap(target);
            handleForwardObstacles();
        }

        tryMoveToTarget(target, currentTime);
    }

    ///  ================= service functions ==========================
    private boolean tryBuildBlock(BlockPos blockPos)
    {
        if (level.getGameTime() < lastBuildTick + (long) (Config.BUILD_COOLDOWN.get() * 20.0f)) return false;
        if (!canBuild(blockPos)) return false;

        level.setBlockAndUpdate(blockPos, bridgeBlock.defaultBlockState());
        level.playSound(null, blockPos, placeSound, SoundSource.BLOCKS, 0.5f, 1.0f);
        freeze();
        lastBuildTick = level.getGameTime();
        BlockStorage.addBuild((ServerLevel) level, blockPos.immutable());
        return true;
    }

    private void tryBreak(BlockPos pos)
    {
        if (level.getGameTime() < lastBreakTick + (long) (Config.BREAK_COOLDOWN.get() * 20.0f)) return;
        if (BlockStorage.buildMapContains((ServerLevel) level, pos)) return;
        if (!((ServerLevel) level).getGameRules().get(GameRules.MOB_GRIEFING)) return;

        BlockState state = level.getBlockState(pos);
        int blockHealth = getBlockHealth(pos);
        int damageGave = BlockStorage.addDamage((ServerLevel) level, pos, Config.DAMAGE_TO_BLOCKS.get());

        if (state.isAir()) return;
        if (blockHealth == Integer.MAX_VALUE) return;

        if (damageGave >= blockHealth)
        {
            BlockStorage.removeDamageData((ServerLevel) level, pos);
            level.destroyBlock(pos, true);
            level.playSound(null, pos, breakSound, SoundSource.HOSTILE, 0.25f, 1.0f);
        }
        else
        {
            level.levelEvent(2001, pos, Block.getId(state)); // particles
            level.playSound(null, pos, hitSound, SoundSource.HOSTILE, 0.25f, 1.0f);
        }
        freeze();
        lastBreakTick = level.getGameTime();
    }

    private void handleVerticalActions(LivingEntity target)
    {
        int selfX = Mth.floor(mob.getX());
        int selfY = Mth.floor(mob.getY());
        int selfZ = Mth.floor(mob.getZ());

        int targetY = Mth.floor(target.getY());

        // target is higher -> we break above/build under
        if (targetY > selfY + 1)
        {
            if (!isFreePass(tickTmpBlockPos.set(selfX, selfY + 1, selfZ))) tryBreak(tickTmpBlockPos);
            if (!isFreePass(tickTmpBlockPos.set(selfX, selfY + 2, selfZ))) tryBreak(tickTmpBlockPos);

            // если сверху свободно — пробуем подстроиться и прыгнуть
            if (isFreePass(tickTmpBlockPos.set(selfX, selfY + 2, selfZ)))
            {
                if (tryBuildBlock(tickTmpBlockPos.set(selfX, selfY, selfZ)))
                {
                    mob.getJumpControl().jump();
                }
            }
            return;
        }

        // target below -> break the block below you (if it's preventing you from getting down)
        if (targetY < selfY - 1)
        {
            if (!isFreePass(tickTmpBlockPos.set(selfX, selfY - 1, selfZ)))
            {
                tryBreak(tickTmpBlockPos);
            }
        }
    }

    private void handleBridgeGap(LivingEntity target)
    {
        int selfX = Mth.floor(mob.getX());
        int selfY = Mth.floor(mob.getY());
        int selfZ = Mth.floor(mob.getZ());

        int targetX = Mth.floor(target.getX());
        int targetZ = Mth.floor(target.getZ());

        int dx = targetX - selfX;
        int dz = targetZ - selfZ;

        int dirX = 0, dirZ = 0;
        if (Math.abs(dx) > Math.abs(dz)) dirX = Integer.signum(dx);
        else if (dz != 0) dirZ = Integer.signum(dz);

        tickFrontBlock.set(selfX + dirX, selfY, selfZ + dirZ);

        // If there is air in front and air underneath (2 blocks) -> we set the bridge to -1
        boolean frontAir = level.getBlockState(tickFrontBlock).isAir();
        boolean belowAir = level.getBlockState(tickTmpBlockPos.set(tickFrontBlock.getX(), tickFrontBlock.getY() - 1, tickFrontBlock.getZ())).isAir();
        boolean below2Air = level.getBlockState(tickTmpBlockPos.set(tickFrontBlock.getX(), tickFrontBlock.getY() - 2, tickFrontBlock.getZ())).isAir();

        if (frontAir && belowAir && below2Air)
        {
            tryBuildBlock(tickTmpBlockPos.set(tickFrontBlock.getX(), tickFrontBlock.getY() - 1, tickFrontBlock.getZ()));
        }
    }

    private void handleForwardObstacles()
    {
        // we break the block right in front of us if it is impassable
        if (!isFreePass(tickFrontBlock))
        {
            tryBreak(tickFrontBlock);
        }

        // we break the block above the front one if it also interferes
        tickTmpBlockPos.set(tickFrontBlock.getX(), tickFrontBlock.getY() + 1, tickFrontBlock.getZ());
        if (!isFreePass(tickTmpBlockPos))
        {
            tryBreak(tickTmpBlockPos);
        }
    }

    private void tryMoveToTarget(LivingEntity target, long currentTime)
    {
        if (freezeUntilTick >= currentTime) return;
        if (currentTime < nextGoToTargetTick) return;

        mob.getNavigation().moveTo(target, 1.0);
        nextGoToTargetTick = currentTime + SecondsToTicksUtility.toTicks(Config.GO_TO_TARGET_INTERVAL.get(), 1);
    }

    // Note: In Minecraft 1.21, "path.canReach()" now returns whether the target can be reached, not whether the next point can be reached.
    // This is why zombies immediately start building with the old function, so I had to change it.
    private void checkPath()
    {
        PathNavigation nav = mob.getNavigation();
        Path path = nav.getPath();

        if (path == null)
        {
            isBreakAndBuild = true;
            return;
        }

        boolean hasActivePath = !path.isDone() && path.getNodeCount() > 0;
        boolean isStuckTooLong = stuckTicks >= (int) (Config.STUCK_SECONDS_BEFORE_BREAKANDBUILD.get() * 20);

        if (hasActivePath && !isStuckTooLong)
        {
            Node endNode = path.getEndNode();

            if (endNode != null)
            {
                double endNodeDistanceSq = mob.distanceToSqr(endNode.x + 0.5, endNode.y, endNode.z + 0.5);

                if (endNodeDistanceSq > 2 * 2)
                {
                    isBreakAndBuild = false;
                    return;
                }
            }
            else
            {
                isBreakAndBuild = false;
                return;
            }
        }

        isBreakAndBuild = true;
    }

    private void freeze()
    {
        freezeUntilTick = (level.getGameTime() + SecondsToTicksUtility.toTicks(Config.FREEZE_TIME.get()));

        mob.getNavigation().stop();
        mob.getMoveControl().setWantedPosition(mob.getX(), mob.getY(), mob.getZ(), 0.0);
        mob.setDeltaMovement(0.0, mob.getDeltaMovement().y, 0.0);
    }

    private void mitigateNearbyDanger()
    {
        int radius = Config.DANGEROUS_SCAN_RADIUS.get();
        BlockPos mobPos = mob.getOnPos();

        int baseX = mobPos.getX();
        int baseY = mobPos.getY();
        int baseZ = mobPos.getZ();

        for (int dy = -1; dy <= 1; dy++)
        {
            for (int dx = -radius; dx <= radius; dx++)
            {
                for (int dz = -radius; dz <= radius; dz++)
                {
                    mndBlockPos.set(baseX + dx, baseY + dy, baseZ + dz);
                    BlockState blockState = level.getBlockState(mndBlockPos);

                    if (!isDangerous(blockState)) continue;

                    mndCover.set(mndBlockPos.getX(), mndBlockPos.getY() + 1, mndBlockPos.getZ());

                    if (canBuild(mndBlockPos))
                    {
                        tryBuildBlock(mndBlockPos);
                    }
                    else
                    {
                        tryBuildBlock(mndCover);
                    }
                }
            }
        }
    }

    private boolean isDangerous(BlockState state)
    {
        if (!state.getFluidState().isEmpty()) return true;
        if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) return true;
        if (state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE)) return state.getValue(CampfireBlock.LIT);
        if (state.is(Blocks.CACTUS) || state.is(Blocks.MAGMA_BLOCK)) return true;
        if (state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.WITHER_ROSE)) return true;
        if (state.is(Blocks.POWDER_SNOW)) return true;

        Identifier id = ForgeRegistries.BLOCKS.getKey(state.getBlock());

        return Config.EXTRA_DANGEROUS_BLOCKS_SET.contains(id);
    }

    private int getBlockHealth(BlockPos blockPos)
    {
        BlockState blockState = level.getBlockState(blockPos);
        float hardness = blockState.getDestroySpeed(level, blockPos);
        if (hardness < 0) return Integer.MAX_VALUE;
        if (hardness != Integer.MAX_VALUE) hardness = Math.min(hardness, 50.0f);
        return hardness != Integer.MAX_VALUE ? Math.max(2, (int) (hardness * 6.0f)) : Integer.MAX_VALUE;
    }

    private boolean canBuild(BlockPos pos)
    {
        BlockState blockState = level.getBlockState(pos);

        boolean mobGriefing = ((ServerLevel) level).getGameRules().get(GameRules.MOB_GRIEFING);
        boolean canReplaced = level.isLoaded(pos) && blockState.canBeReplaced();
        boolean isAir = level.isLoaded(pos) && blockState.isAir();

        return mobGriefing && (canReplaced || isAir);
    }

    private boolean isFreePass(BlockPos pos)
    {
        BlockState blockState = level.getBlockState(pos);
        if (blockState.isAir()) return true;
        return blockState.getCollisionShape(level, pos).isEmpty();
    }

    private void updateStuckState(LivingEntity target, long currentTime)
    {
        double distSq = mob.distanceToSqr(target);
        if (Double.isNaN(lastDistSq)) lastDistSq = distSq;

        if (freezeUntilTick < currentTime)
        {
            if (distSq < lastDistSq - 0.25)
            {
                stuckTicks = 0;
                isBreakAndBuild = false;
            }
            else
            {
                stuckTicks++;
            }
            lastDistSq = distSq;
        }
    }


    /// ==================================================================================

    @Override
    public boolean canUse()
    {
        LivingEntity tgt = mob.getTarget();
        return tgt != null && tgt.isAlive();
    }
}
