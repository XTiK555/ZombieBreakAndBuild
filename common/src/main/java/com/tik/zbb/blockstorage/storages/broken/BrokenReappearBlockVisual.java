package com.tik.zbb.blockstorage.storages.broken;

import com.tik.zbb.Constants;
import com.tik.zbb.config.ConfigGame;
import com.tik.zbb.config.ConfigManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import org.greenrobot.eventbus.Subscribe;

public class BrokenReappearBlockVisual
{
    private static final int REAPPEAR_PARTICLE_STEPS = 15;
    private static final int REAPPEAR_PARTICLE_STEP_DELAY = 1;
    private static final double REAPPEAR_PARTICLE_BLOCK_START_SCALE = 1.5;
    private static final double REAPPEAR_PARTICLE_BLOCK_END_SCALE = 1.0;
    private static final int REAPPEAR_PARTICLE_BLOCK_GRID = 3;
    private static final boolean REAPPEAR_PARTICLE_BLOCK_SURFACE_ONLY = true;
    private static final double REAPPEAR_PARTICLE_PARTICLE_JITTER = 0;
    private static final double REAPPEAR_PARTICLE_PARTICLE_SPEED = 0;

    private static final float REAPPEAR_SOUND_VOLUME = 0.1f;
    private static final float REAPPEAR_SOUND_PITCH = 0.08f;

    private static final float REAPPEAR_CHARGE_SOUND_VOLUME = 0.2f;
    private static final float REAPPEAR_CHARGE_SOUND_PITCH = 0.01f;

    @Subscribe
    public void onBrokenBlockWillReappear(BrokenReappearBlockStorageManager.OnBrokenBlockWillReappearEvent event)
    {
        ConfigGame.VisualEffects visualEffects = ConfigManager.getConfigSnapshot().game().visualEffects();

        if (visualEffects.brokenReappearParticles()) playReappearAssemblingBlockEffect(event);
        if (visualEffects.brokenReappearChargeSound()) playReappearChargeSound(event);
    }

    @Subscribe
    public void onBrokenBlockReappear(BrokenReappearBlockStorageManager.OnBrokenBlockReappearEvent event)
    {
        ConfigGame.VisualEffects visualEffects = ConfigManager.getConfigSnapshot().game().visualEffects();

        if (visualEffects.brokenReappearSound()) playReappearSound(event);
    }

    private void playReappearAssemblingBlockEffect(BrokenReappearBlockStorageManager.OnBrokenBlockWillReappearEvent event)
    {
        ServerLevel level = event.level();
        BlockPos pos = event.pos();
        BlockState state = event.newState();

        double centerX = pos.getX() + 0.5;
        double centerY = pos.getY() + 0.5;
        double centerZ = pos.getZ() + 0.5;

        for (int step = 0; step < REAPPEAR_PARTICLE_STEPS; step++)
        {
            final int currentStep = step;

            Constants.SCHEDULER.schedule(() ->
            {
                double progress = (currentStep + 1) / (double) REAPPEAR_PARTICLE_STEPS;

                double eased = easeOut(progress);
                double scale = lerp(REAPPEAR_PARTICLE_BLOCK_START_SCALE, REAPPEAR_PARTICLE_BLOCK_END_SCALE, eased);

                spawnBlockShapeParticles(level, state, centerX, centerY, centerZ, scale);

            }, currentStep * REAPPEAR_PARTICLE_STEP_DELAY);
        }
    }

    private void spawnBlockShapeParticles(ServerLevel level, BlockState state, double centerX, double centerY, double centerZ, double scale)
    {
        BlockParticleOption particle = new BlockParticleOption(ParticleTypes.BLOCK, state);

        int grid = Math.max(2, REAPPEAR_PARTICLE_BLOCK_GRID);
        double half = scale * 0.5;
        double step = scale / (grid - 1);

        for (int xi = 0; xi < grid; xi++)
        {
            for (int yi = 0; yi < grid; yi++)
            {
                for (int zi = 0; zi < grid; zi++)
                {
                    boolean isSurface =
                            xi == 0 || xi == grid - 1 ||
                                    yi == 0 || yi == grid - 1 ||
                                    zi == 0 || zi == grid - 1;

                    if (REAPPEAR_PARTICLE_BLOCK_SURFACE_ONLY && !isSurface)
                        continue;

                    double x = centerX - half + xi * step;
                    double y = centerY - half + yi * step;
                    double z = centerZ - half + zi * step;

                    level.sendParticles(
                            particle,
                            x, y, z,
                            1,
                            REAPPEAR_PARTICLE_PARTICLE_JITTER,
                            REAPPEAR_PARTICLE_PARTICLE_JITTER,
                            REAPPEAR_PARTICLE_PARTICLE_JITTER,
                            REAPPEAR_PARTICLE_PARTICLE_SPEED
                    );
                }
            }
        }
    }

    private void playReappearSound(BrokenReappearBlockStorageManager.OnBrokenBlockReappearEvent event)
    {
        event.level().playSound(
                null,
                event.pos(),
                SoundEvents.ITEM_PICKUP,
                SoundSource.BLOCKS,
                REAPPEAR_SOUND_VOLUME,
                REAPPEAR_SOUND_PITCH
        );
    }

    private void playReappearChargeSound(BrokenReappearBlockStorageManager.OnBrokenBlockWillReappearEvent event)
    {
        event.level().playSound(
                null,
                event.pos(),
                SoundEvents.BREEZE_CHARGE,
                SoundSource.BLOCKS,
                REAPPEAR_CHARGE_SOUND_VOLUME,
                REAPPEAR_CHARGE_SOUND_PITCH
        );
    }

    private static double lerp(double start, double end, double progress)
    {
        return start + (end - start) * progress;
    }

    private static double easeOut(double t)
    {
        return 1.0 - Math.pow(1.0 - t, 2.0);
    }
}
