package com.tik.zbb.gametest.fixture;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.tik.zbb.Constants;
import com.tik.zbb.config.ConfigManager;
import com.tik.zbb.config.edit.ConfigEditRequest;
import com.tik.zbb.config.edit.ConfigEditResult;
import com.tik.zbb.config.schema.ConfigPath;
import net.minecraft.gametest.framework.GameTestHelper;

public final class GameTestConfig
{
    private GameTestConfig() {}

    public static void set(GameTestHelper helper, String path, Object value)
    {
        edit(helper, ConfigEditRequest.set(path(path), String.valueOf(value)));
    }

    public static void put(GameTestHelper helper, String path, Object key, Object value)
    {
        edit(helper, ConfigEditRequest.add(path(path), key + "=" + value));
    }

    public static void add(GameTestHelper helper, String path, Object value)
    {
        edit(helper, ConfigEditRequest.add(path(path), String.valueOf(value)));
    }

    public static void remove(GameTestHelper helper, String path, Object value)
    {
        edit(helper, ConfigEditRequest.remove(path(path), String.valueOf(value)));
    }

    public static void clear(GameTestHelper helper, String path)
    {
        edit(helper, ConfigEditRequest.clear(path(path)));
    }

    public static void reset(GameTestHelper helper, String path)
    {
        edit(helper, ConfigEditRequest.reset(path(path)));
    }

    public static void accelerateActions(GameTestHelper helper)
    {
        set(helper, "balance.blockDamage.damageToBlocks", 100);
        set(helper, "balance.cooldowns.breakCooldown", 0);
        set(helper, "balance.cooldowns.buildCooldown", 0);
        set(helper, "balance.cooldowns.searchDangerousBlocksCooldown", 0);
    }

    public static void runConfigCommandExpectingChange(GameTestHelper helper, String arguments)
    {
        helper.assertTrue(executeCommand(helper, Constants.MOD_ID + " config " + arguments) > 0,
                "Config command failed: " + arguments);
    }

    private static int executeCommand(GameTestHelper helper, String command)
    {
        try
        {
            return helper.getLevel().getServer().getCommands().getDispatcher().execute(
                    command, helper.getLevel().getServer().createCommandSourceStack());
        }
        catch (CommandSyntaxException exception)
        {
            throw new AssertionError("Could not execute command: " + command, exception);
        }
    }

    public static void restoreDefaults(GameTestHelper helper)
    {
        edit(helper, ConfigEditRequest.resetAll());
    }

    private static ConfigPath path(String path)
    {
        return new ConfigPath(path);
    }

    private static void edit(GameTestHelper helper, ConfigEditRequest request)
    {
        ConfigEditResult result = ConfigManager.editRaw(request);
        helper.assertTrue(result.success(), "Config edit failed: " + result.message());
    }
}
