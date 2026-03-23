package com.tik.zbb.blockstorage.storages.buildDisappear;

import com.tik.zbb.Constants;
import com.tik.zbb.config.ConfigData;
import com.tik.zbb.config.ConfigManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.greenrobot.eventbus.Subscribe;

import java.util.Locale;
import java.util.UUID;

public class BuildDisappearBlockVisual
{
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
        ConfigData configData = ConfigManager.getConfigSnapshot().data();

        if (configData.visualEffects.builtDisappearBlockDisplay) playShrinkBlockDisplayEffect(event);
        if (configData.visualEffects.builtDisappearShrinkSound) playShrinkSound(event);
        if (configData.visualEffects.builtDisappearSound)
            Constants.SCHEDULER.schedule(() -> playDisappearSound(event), (SHRINK_BLOCK_DISPLAY_STEPS * SHRINK_BLOCK_DISPLAY_STEP_DELAY) + SHRINK_BLOCK_DISPLAY_STEP_DELAY);
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
        MinecraftServer server = event.level().getServer();
        if (server == null) return;

        double x = event.pos().getX() + 0.5;
        double y = event.pos().getY();
        double z = event.pos().getZ() + 0.5;

        CommandSourceStack source = server.createCommandSourceStack()
                .withSuppressedOutput()
                .withLevel(event.level());

        UUID uuid = UUID.randomUUID();
        String uuidNbt =
                "[I;" +
                        (int) (uuid.getMostSignificantBits() >> 32) + "," +
                        (int) uuid.getMostSignificantBits() + "," +
                        (int) (uuid.getLeastSignificantBits() >> 32) + "," +
                        (int) uuid.getLeastSignificantBits() +
                        "]";

        CompoundTag blockStateTag = NbtUtils.writeBlockState(event.placedState());

        server.getCommands().performPrefixedCommand(source,
                "summon minecraft:block_display " + formatDouble(x) + " " + formatDouble(y) + " " + formatDouble(z) + " " +
                        "{UUID:" + uuidNbt + "," +
                        "block_state:" + blockStateTag + "," +
                        "transformation:{" +
                        "translation:[-0.5f,0.0f,-0.5f]," +
                        "left_rotation:[0.0f,0.0f,0.0f,1.0f]," +
                        "scale:[" + formatFloat(SHRINK_BLOCK_DISPLAY_START_SCALE) + "f," + formatFloat(SHRINK_BLOCK_DISPLAY_START_SCALE) + "f," + formatFloat(SHRINK_BLOCK_DISPLAY_START_SCALE) + "f]," +
                        "right_rotation:[0.0f,0.0f,0.0f,1.0f]" +
                        "}," +
                        "start_interpolation:0," +
                        "interpolation_duration:1" +
                        "}"
        );

        for (int step = 1; step <= SHRINK_BLOCK_DISPLAY_STEPS; step++)
        {
            final int currentStep = step;

            Constants.SCHEDULER.schedule(() ->
            {
                MinecraftServer scheduledServer = event.level().getServer();
                if (scheduledServer == null) return;

                CommandSourceStack scheduledSource = scheduledServer.createCommandSourceStack()
                        .withSuppressedOutput()
                        .withLevel(event.level());

                float progress = currentStep / (float) SHRINK_BLOCK_DISPLAY_STEPS;
                float easedProgress = easeIn(progress, SHRINK_BLOCK_DISPLAY_CURVE_POWER);

                float scale = lerp(
                        SHRINK_BLOCK_DISPLAY_START_SCALE,
                        SHRINK_BLOCK_DISPLAY_END_SCALE,
                        easedProgress
                );

                float tx = -0.5f * scale;
                float tz = -0.5f * scale;

                scheduledServer.getCommands().performPrefixedCommand(scheduledSource,
                        "data merge entity @e[type=minecraft:block_display,limit=1,nbt={UUID:" + uuidNbt + "}] " +
                                "{start_interpolation:0,interpolation_duration:" + SHRINK_BLOCK_DISPLAY_STEP_DELAY + "," +
                                "transformation:{" +
                                "translation:[" + formatFloat(tx) + "f,0.0f," + formatFloat(tz) + "f]," +
                                "left_rotation:[0.0f,0.0f,0.0f,1.0f]," +
                                "scale:[" + formatFloat(scale) + "f," + formatFloat(scale) + "f," + formatFloat(scale) + "f]," +
                                "right_rotation:[0.0f,0.0f,0.0f,1.0f]" +
                                "}" +
                                "}"
                );

            }, currentStep * SHRINK_BLOCK_DISPLAY_STEP_DELAY);
        }

        Constants.SCHEDULER.schedule(() ->
        {
            MinecraftServer scheduledServer = event.level().getServer();
            if (scheduledServer == null) return;

            CommandSourceStack scheduledSource = scheduledServer.createCommandSourceStack()
                    .withSuppressedOutput()
                    .withLevel(event.level());

            scheduledServer.getCommands().performPrefixedCommand(scheduledSource,
                    "kill @e[type=minecraft:block_display,limit=1,nbt={UUID:" + uuidNbt + "}]"
            );

        }, (SHRINK_BLOCK_DISPLAY_STEPS * SHRINK_BLOCK_DISPLAY_STEP_DELAY) + SHRINK_BLOCK_DISPLAY_STEP_DELAY);
    }

    private static float easeIn(float t, float power)
    {
        return (float) Math.pow(t, power);
    }

    private static float lerp(float start, float end, float progress)
    {
        return start + (end - start) * progress;
    }

    private static String formatFloat(float value)
    {
        return String.format(Locale.ROOT, "%.4f", value);
    }

    private static String formatDouble(double value)
    {
        return String.format(Locale.ROOT, "%.4f", value);
    }
}