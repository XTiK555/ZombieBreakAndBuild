package com.tik.zbb.gametest.fixture;

import com.mojang.math.Transformation;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Display;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.reflect.Method;

public final class DisplayProbes
{
    private DisplayProbes() {}

    public static Transformation transformation(Display display)
    {
        return invoke(Display.class, null, "createTransformation",
                new Class<?>[]{SynchedEntityData.class}, display.getEntityData());
    }

    public static int transformationInterpolationDuration(Display display)
    {
        return invoke(Display.class, display, "getInterpolationDuration", new Class<?>[0]);
    }

    public static int transformationInterpolationDelay(Display display)
    {
        return invoke(Display.class, display, "getInterpolationDelay", new Class<?>[0]);
    }

    public static BlockState blockState(Display.BlockDisplay display)
    {
        return invoke(Display.BlockDisplay.class, display, "getBlockState", new Class<?>[0]);
    }

    @SuppressWarnings("unchecked")
    private static <T> T invoke(Class<?> owner, Object target, String name, Class<?>[] parameterTypes,
                                Object... arguments)
    {
        try
        {
            Method method = owner.getDeclaredMethod(name, parameterTypes);
            method.setAccessible(true);
            return (T) method.invoke(target, arguments);
        }
        catch (ReflectiveOperationException exception)
        {
            throw new AssertionError("Cannot inspect " + owner.getSimpleName() + "." + name, exception);
        }
    }
}
