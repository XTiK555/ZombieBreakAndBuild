package com.tik.zbb.blockstorage.storages.buildDisappear;

import com.mojang.math.Transformation;
import com.tik.zbb.Constants;
import com.tik.zbb.MainCommon;
import com.tik.zbb.config.ConfigGame;
import com.tik.zbb.config.ConfigManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.storage.TagValueInput;
import org.greenrobot.eventbus.Subscribe;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class BuildDisappearBlockVisual
{
    private static final String DISPLAY_TAG = "zbb_build_disappear";
    private static final int SHRINK_BLOCK_DISPLAY_STEPS = 8;
    private static final int SHRINK_BLOCK_DISPLAY_STEP_DELAY = 1;
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
            Constants.SCHEDULER.schedule(() -> playDisappearSound(event), (SHRINK_BLOCK_DISPLAY_STEPS * SHRINK_BLOCK_DISPLAY_STEP_DELAY) + SHRINK_BLOCK_DISPLAY_STEP_DELAY);
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

        Display.BlockDisplay blockDisplay = new Display.BlockDisplay(EntityTypes.BLOCK_DISPLAY, event.level())
        {
            @Override
            public boolean shouldBeSaved()
            {
                return false;
            }
        };

        updateBlockDisplay(blockDisplay, event, x, y, z, SHRINK_BLOCK_DISPLAY_START_SCALE, 1);
        if (!event.level().addFreshEntity(blockDisplay)) return;

        for (int step = 1; step <= SHRINK_BLOCK_DISPLAY_STEPS; step++)
        {
            final int currentStep = step;

            Constants.SCHEDULER.schedule(() ->
            {
                if (blockDisplay.isRemoved()) return;

                float progress = currentStep / (float) SHRINK_BLOCK_DISPLAY_STEPS;
                float easedProgress = easeIn(progress, SHRINK_BLOCK_DISPLAY_CURVE_POWER);

                float scale = lerp(
                        SHRINK_BLOCK_DISPLAY_START_SCALE,
                        SHRINK_BLOCK_DISPLAY_END_SCALE,
                        easedProgress
                );

                updateBlockDisplay(blockDisplay, event, x, y, z, scale, SHRINK_BLOCK_DISPLAY_STEP_DELAY);

            }, currentStep * SHRINK_BLOCK_DISPLAY_STEP_DELAY);
        }

        Constants.SCHEDULER.schedule(blockDisplay::discard,
                (SHRINK_BLOCK_DISPLAY_STEPS * SHRINK_BLOCK_DISPLAY_STEP_DELAY) + SHRINK_BLOCK_DISPLAY_STEP_DELAY);
    }

    private void updateBlockDisplay(Display.BlockDisplay blockDisplay,
                                    BuildDisappearBlockStorageManager.OnBuildBlockDisappearEvent event,
                                    double x, double y, double z, float scale, int interpolationDuration)
    {
        float translation = -0.5f * scale;
        Transformation transformation = new Transformation(
                new Vector3f(translation),
                new Quaternionf(),
                new Vector3f(scale),
                new Quaternionf()
        );

        CompoundTag displayData = new CompoundTag();
        displayData.put(Display.BlockDisplay.TAG_BLOCK_STATE, NbtUtils.writeBlockState(event.placedState()));
        displayData.put(Display.TAG_TRANSFORMATION,
                Transformation.CODEC.encodeStart(NbtOps.INSTANCE, transformation).getOrThrow());
        displayData.putInt(Display.TAG_TRANSFORMATION_START_INTERPOLATION, 0);
        displayData.putInt(Display.TAG_TRANSFORMATION_INTERPOLATION_DURATION, interpolationDuration);

        blockDisplay.load(TagValueInput.create(
                ProblemReporter.DISCARDING,
                event.level().registryAccess(),
                displayData
        ));
        blockDisplay.setPos(x, y, z);
        blockDisplay.addTag(DISPLAY_TAG);
    }

    private static float easeIn(float t, float power)
    {
        return (float) Math.pow(t, power);
    }

    private static float lerp(float start, float end, float progress)
    {
        return start + (end - start) * progress;
    }

}
