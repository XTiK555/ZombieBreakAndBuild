package com.tik.zbb.blockstorage.storages.buildDisappear;

import com.mojang.math.Transformation;
import com.tik.zbb.Constants;
import com.tik.zbb.MainCommon;
import com.tik.zbb.config.ConfigGame;
import com.tik.zbb.config.ConfigManager;
import com.tik.zbb.mixin.accessor.display.BlockDisplayAccessor;
import com.tik.zbb.mixin.accessor.display.DisplayAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityTypes;
import org.greenrobot.eventbus.Subscribe;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class BuildDisappearBlockStorageVisual
{
    private static final String DISPLAY_TAG = "zbb_build_disappear";
    private static final int SHRINK_BLOCK_START_DELAY = 2;
    private static final int SHRINK_BLOCK_INTERPOLATION_DURATION = 8;
    private static final int SHRINK_BLOCK_DISPLAY_LIFETIME = SHRINK_BLOCK_START_DELAY + SHRINK_BLOCK_INTERPOLATION_DURATION;
    private static final float SHRINK_BLOCK_DISPLAY_START_SCALE = 1.0f;
    private static final float SHRINK_BLOCK_DISPLAY_END_SCALE = 0.05f;
    private static final float SHRINK_BLOCK_DISPLAY_CURVE_POWER = 1.6f;

    private static final float SHRINK_SOUND_VOLUME = 0.1f;
    private static final float SHRINK_SOUND_PITCH = 0.1f;

    private static final float DISAPPEAR_SOUND_VOLUME = 0.15f;
    private static final float DISAPPEAR_SOUND_PITCH = 0.05f;

    @Subscribe
    public void onBuildBlockDisappear(BuildDisappearBlockStorageManager.OnBuildBlockDisappearEvent event)
    {
        ConfigGame.VisualEffects visualEffects = ConfigManager.getConfigSnapshot().game().visualEffects();

        if (visualEffects.builtDisappearBlockDisplay()) playShrinkBlockDisplayEffect(event);
        if (visualEffects.builtDisappearShrinkSound()) playShrinkSound(event);
        if (visualEffects.builtDisappearSound())
            Constants.SCHEDULER.schedule(() -> playDisappearSound(event), SHRINK_BLOCK_START_DELAY + SHRINK_BLOCK_INTERPOLATION_DURATION);
    }

    @Subscribe
    public void onServerStopping(MainCommon.OnServerStoppingEvent event)
    {
        for (ServerLevel level : event.server().getAllLevels())
        {
            level.getAllEntities().forEach(entity ->
            {
                if (entity instanceof Display.BlockDisplay blockDisplay)
                {
                    if (blockDisplay.entityTags().contains(DISPLAY_TAG))
                    {
                        blockDisplay.discard();
                    }
                }
            });
        }
    }

    private void playDisappearSound(BuildDisappearBlockStorageManager.OnBuildBlockDisappearEvent event)
    {
        event.level().playSound(
                null,
                event.pos(),
                SoundEvents.ITEM_PICKUP,
                SoundSource.BLOCKS,
                DISAPPEAR_SOUND_VOLUME,
                DISAPPEAR_SOUND_PITCH
        );
    }

    private void playShrinkSound(BuildDisappearBlockStorageManager.OnBuildBlockDisappearEvent event)
    {
        event.level().playSound(
                null,
                event.pos(),
                SoundEvents.BEEHIVE_SHEAR,
                SoundSource.BLOCKS,
                SHRINK_SOUND_VOLUME,
                SHRINK_SOUND_PITCH
        );
    }

    private void playShrinkBlockDisplayEffect(BuildDisappearBlockStorageManager.OnBuildBlockDisappearEvent event)
    {
        double x = event.pos().getX() + 0.5;
        double y = event.pos().getY() + 0.5;
        double z = event.pos().getZ() + 0.5;

        Display.BlockDisplay blockDisplay =
                new Display.BlockDisplay(EntityTypes.BLOCK_DISPLAY, event.level())
                {
                    @Override
                    public boolean shouldBeSaved()
                    {
                        return false;
                    }

                    @Override
                    public void tick()
                    {
                        super.tick();

                        if (tickCount > SHRINK_BLOCK_START_DELAY && tickCount <= SHRINK_BLOCK_DISPLAY_LIFETIME)
                        {
                            float progress = (tickCount - SHRINK_BLOCK_START_DELAY) / (float) SHRINK_BLOCK_INTERPOLATION_DURATION;
                            float easedProgress = (float) Math.pow(progress, SHRINK_BLOCK_DISPLAY_CURVE_POWER);
                            float scale = SHRINK_BLOCK_DISPLAY_START_SCALE + (SHRINK_BLOCK_DISPLAY_END_SCALE - SHRINK_BLOCK_DISPLAY_START_SCALE) * easedProgress;

                            DisplayAccessor accessor = (DisplayAccessor) (Object) this;
                            accessor.zbb$setTransformationInterpolationDuration(1);
                            accessor.zbb$setTransformation(createTransformation(scale));
                            accessor.zbb$setTransformationInterpolationDelay(0);
                        }

                        if (tickCount > SHRINK_BLOCK_DISPLAY_LIFETIME + 1)
                        {
                            discard();
                        }
                    }
                };

        DisplayAccessor displayAccessor = (DisplayAccessor) blockDisplay;
        BlockDisplayAccessor blockDisplayAccessor = (BlockDisplayAccessor) blockDisplay;

        blockDisplayAccessor.zbb$setBlockState(event.placedState());

        displayAccessor.zbb$setTransformation(createTransformation(SHRINK_BLOCK_DISPLAY_START_SCALE));
        displayAccessor.zbb$setTransformationInterpolationDuration(0);
        displayAccessor.zbb$setTransformationInterpolationDelay(0);

        blockDisplay.setPos(x, y, z);
        blockDisplay.addTag(DISPLAY_TAG);

        if (!event.level().addFreshEntity(blockDisplay)) return;

        Constants.SCHEDULER.schedule(() ->
        {
            if (blockDisplay != null)
                blockDisplay.discard();
        }, SHRINK_BLOCK_DISPLAY_LIFETIME + 2);
    }

    private Transformation createTransformation(float scale)
    {
        float translation = -0.5f * scale;

        return new Transformation(new Vector3f(translation), new Quaternionf(), new Vector3f(scale), new Quaternionf());
    }
}
