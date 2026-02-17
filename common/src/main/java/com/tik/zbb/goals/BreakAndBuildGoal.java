package com.tik.zbb.goals;

import com.tik.zbb.BlockStorage;
import com.tik.zbb.config.ConfigManager;
import com.tik.zbb.config.ConfigData;
import com.tik.zbb.utilities.SecondsToTicksUtility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
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

public class BreakAndBuildGoal extends Goal
{
    private final PathfinderMob mob;
    private final Level level;
    private final Block bridgeBlock;
    private final SoundEvent placeSound, hitSound, breakSound;
    private final ConfigData config;
    private final Registry<Block> blockRegistry;
    private final Registry<SoundEvent> soundEventRegistry;

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
    private final BlockPos.MutableBlockPos tickFunctionTmpBlockPos = new BlockPos.MutableBlockPos();

    // carried out from mitigateNearbyDanger for optimization
    BlockPos.MutableBlockPos mndBlockPos = new BlockPos.MutableBlockPos();
    BlockPos.MutableBlockPos mndCover = new BlockPos.MutableBlockPos();

    public BreakAndBuildGoal(PathfinderMob mob)
    {
        this.mob = mob;
        this.level = mob.level();
        this.config = ConfigManager.getConfigData();

        this.blockRegistry = level.registryAccess().lookupOrThrow(Registries.BLOCK);
        this.soundEventRegistry = level.registryAccess().lookupOrThrow(Registries.SOUND_EVENT);

        Identifier blockId = Identifier.tryParse(config.bridgeBlockId);
        if (blockId != null)
            this.bridgeBlock = blockRegistry.get(blockId).map(Holder.Reference::value).orElse(Blocks.DIRT);
        else this.bridgeBlock = null;

        Identifier placeSoundId = Identifier.tryParse(config.placeSoundId);
        if (placeSoundId != null)
            this.placeSound = soundEventRegistry.get(placeSoundId).map(Holder.Reference::value).orElse(SoundEvents.ROOTED_DIRT_PLACE);
        else this.placeSound = null;

        Identifier hitSoundId = Identifier.tryParse(config.hitSoundId);
        if (hitSoundId != null)
            this.hitSound = soundEventRegistry.get(hitSoundId).map(Holder.Reference::value).orElse(SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR);
        else this.hitSound = null;

        Identifier breakSoundId = Identifier.tryParse(config.breakSoundId);
        if (breakSoundId != null)
            this.breakSound = soundEventRegistry.get(breakSoundId).map(Holder.Reference::value).orElse(SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR);
        else this.breakSound = null;
    }

    @Override
    public void tick()
    {
        LivingEntity target = mob.getTarget();
        if (target == null) return;

        final long currentTime = level.getGameTime();

        updateStuckState(target, currentTime);

        if (currentTime >= nextPathCheckTick)
        {
            checkPath();
            nextPathCheckTick = level.getGameTime() + SecondsToTicksUtility.toTicks(config.pathCheckInterval, 1);
        }

        if (isBreakAndBuild)
        {
            int selfX = Mth.floor(mob.getX());
            int selfY = Mth.floor(mob.getY());
            int selfZ = Mth.floor(mob.getZ());

            int targetX = Mth.floor(target.getX());
            int targetZ = Mth.floor(target.getZ());
            int targetY = Mth.floor(target.getY());

            int dx = targetX - selfX;
            int dz = targetZ - selfZ;

            int dirX = 0, dirZ = 0;
            if (Math.abs(dx) > Math.abs(dz)) dirX = Integer.signum(dx);
            else if (dz != 0) dirZ = Integer.signum(dz);

            handleVerticalActions(tickTmpBlockPos.set(selfX, selfY, selfZ), targetY);
            handleBridgeGap(tickTmpBlockPos.set(selfX + dirX, selfY, selfZ + dirZ));
            handleForwardObstacles(tickTmpBlockPos, selfX, selfY, selfZ);
        }

        if (currentTime >= nextSearchDangerousTick)
        {
            mitigateNearbyDanger();
            nextSearchDangerousTick = level.getGameTime() + SecondsToTicksUtility.toTicks(config.searchDangerousBlocksInterval, 1);
        }

        tryMoveToTarget(target, currentTime);
    }

    ///  ================= local functions ==========================
    private boolean tryBuildBlock(BlockPos blockPos)
    {
        if (level.getGameTime() < lastBuildTick + SecondsToTicksUtility.toTicks(config.buildCooldown)) return false;
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
        if (level.getGameTime() < lastBreakTick + SecondsToTicksUtility.toTicks(config.breakCooldown)) return;
        if (BlockStorage.buildMapContains((ServerLevel) level, pos)) return;
        if (!((ServerLevel) level).getGameRules().get(GameRules.MOB_GRIEFING)) return;

        BlockState state = level.getBlockState(pos);
        int blockHealth = getBlockHealth(pos);
        int damageGave = BlockStorage.addDamage((ServerLevel) level, pos, config.damageToBlocks);

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

    private void handleVerticalActions(BlockPos mobBlockPos, int targetY)
    {
        // target is higher -> we break above/build under
        if (targetY > mobBlockPos.getY() + 1)
        {
            if (!isFreePass(tickFunctionTmpBlockPos.set(mobBlockPos.getX(), mobBlockPos.getY() + 1, mobBlockPos.getZ())))
                tryBreak(tickFunctionTmpBlockPos);
            if (!isFreePass(tickFunctionTmpBlockPos.set(mobBlockPos.getX(), mobBlockPos.getY() + 2, mobBlockPos.getZ())))
                tryBreak(tickFunctionTmpBlockPos);

            // If there is space above, we try to adjust and jump.
            if (isFreePass(tickFunctionTmpBlockPos.set(mobBlockPos.getX(), mobBlockPos.getY() + 2, mobBlockPos.getZ())))
            {
                if (tryBuildBlock(tickFunctionTmpBlockPos.set(mobBlockPos.getX(), mobBlockPos.getY(), mobBlockPos.getZ())))
                {
                    mob.getJumpControl().jump();
                }
            }
            return;
        }

        // target below -> break the block below you (if it's preventing you from getting down)
        if (targetY < mobBlockPos.getY() - 1)
        {
            if (!isFreePass(tickFunctionTmpBlockPos.set(mobBlockPos.getX(), mobBlockPos.getY() - 1, mobBlockPos.getZ())))
            {
                tryBreak(tickFunctionTmpBlockPos);
            }
        }
    }

    private void handleBridgeGap(BlockPos frontBlockPos)
    {
        // If there is air in front and air underneath (2 blocks) -> we set the bridge to -1
        boolean frontAir = level.getBlockState(frontBlockPos).isAir();
        boolean belowAir = level.getBlockState(tickFunctionTmpBlockPos.set(frontBlockPos.getX(), frontBlockPos.getY() - 1, frontBlockPos.getZ())).isAir();
        boolean below2Air = level.getBlockState(tickFunctionTmpBlockPos.set(frontBlockPos.getX(), frontBlockPos.getY() - 2, frontBlockPos.getZ())).isAir();

        if (frontAir && belowAir && below2Air)
        {
            tryBuildBlock(tickFunctionTmpBlockPos.set(frontBlockPos.getX(), frontBlockPos.getY() - 1, frontBlockPos.getZ()));
        }
    }

    private void handleForwardObstacles(BlockPos frontBlockPos, int mobX, int mobY, int mobZ)
    {
        tickFunctionTmpBlockPos.set(mobX, mobY, mobZ);
        if (!isFreePass(tickFunctionTmpBlockPos))
        {
            tryBreak(tickFunctionTmpBlockPos);
        }
        tickFunctionTmpBlockPos.move(0, 1, 0);
        if (!isFreePass(tickFunctionTmpBlockPos))
        {
            tryBreak(tickFunctionTmpBlockPos);
        }

        // we break the block right in front of us if it is impassable
        if (!isFreePass(frontBlockPos))
        {
            tryBreak(frontBlockPos);
        }
        // we break the block above the front one if it also interferes
        tickFunctionTmpBlockPos.set(frontBlockPos.getX(), frontBlockPos.getY() + 1, frontBlockPos.getZ());
        if (!isFreePass(tickFunctionTmpBlockPos))
        {
            tryBreak(tickFunctionTmpBlockPos);
        }
    }

    private void tryMoveToTarget(LivingEntity target, long currentTime)
    {
        if (freezeUntilTick >= currentTime) return;
        if (currentTime < nextGoToTargetTick) return;

        mob.getNavigation().moveTo(target, 1.0);
        nextGoToTargetTick = currentTime + SecondsToTicksUtility.toTicks(config.goToTargetInterval, 1);
    }

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
        boolean isStuckTooLong = stuckTicks >= SecondsToTicksUtility.toTicks(config.stuckSecondsBeforeBreakAndBuild, 1);

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
        freezeUntilTick = (level.getGameTime() + SecondsToTicksUtility.toTicks(config.freezeTime));

        mob.getNavigation().stop();
        mob.getMoveControl().setWantedPosition(mob.getX(), mob.getY(), mob.getZ(), 0.0);
        mob.setDeltaMovement(0.0, mob.getDeltaMovement().y, 0.0);
    }

    private void mitigateNearbyDanger()
    {
        int radius = config.dangerousBlocksSearchRadius;
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

        Identifier id = blockRegistry.getKey(state.getBlock());
        if (id == null) return false;

        if (!config.dangerousBlocksSet.contains(id)) return false;

        if (state.getBlock() instanceof CampfireBlock)
        {
            return state.getValue(CampfireBlock.LIT);
        }

        return true;
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
        Identifier id = blockRegistry.getKey(blockState.getBlock());

        if (blockState.isAir()) return true;
        if (id != null && config.impassableBlocksSet.contains(id)) return false;

        return blockState.getCollisionShape(level, pos).isEmpty();
    }

    private void updateStuckState(LivingEntity target, long currentTime)
    {
        double distSq = mob.distanceToSqr(target);
        if (Double.isNaN(lastDistSq)) lastDistSq = distSq;

        if (freezeUntilTick < currentTime)
        {
            if (distSq < lastDistSq - 0.5)
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
