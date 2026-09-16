package com.tik.zbb.gametest;

import com.tik.zbb.gametest.fixture.GameTestConfig;
import com.tik.zbb.gametest.fixture.GameTestEntities;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GlobalTestReporter;
import net.minecraft.gametest.framework.TestReporter;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;

import java.util.IdentityHashMap;
import java.util.Map;

public final class PortabilityGameTestLifecycle
{
    private static final Map<MinecraftServer, Difficulty> ORIGINAL_DIFFICULTIES = new IdentityHashMap<>();
    private static boolean installed;

    private PortabilityGameTestLifecycle() {}

    public static synchronized void install(TestReporter reporter)
    {
        if (installed) return;
        GlobalTestReporter.replaceWith(new CleanupReporter(reporter));
        installed = true;
    }

    static void beginTest(GameTestHelper helper, Difficulty difficulty)
    {
        install(PortabilityGameTestReporter.xmlReporter());
        GameTestEntities.cleanupTracked(helper);
        GameTestConfig.restoreRuntimeDefaults(helper);
        ensureDifficulty(helper, difficulty);
    }

    static void ensureDifficulty(GameTestHelper helper, Difficulty difficulty)
    {
        MinecraftServer server = helper.getLevel().getServer();
        ORIGINAL_DIFFICULTIES.putIfAbsent(server, helper.getLevel().getDifficulty());
        if (helper.getLevel().getDifficulty() != difficulty) server.setDifficulty(difficulty, true);
    }

    private record CleanupReporter(TestReporter delegate) implements TestReporter
    {
        @Override
        public void onTestFailed(GameTestInfo testInfo)
        {
            reportAndCleanup(testInfo, delegate::onTestFailed);
        }

        @Override
        public void onTestSuccess(GameTestInfo testInfo)
        {
            reportAndCleanup(testInfo, delegate::onTestSuccess);
        }

        @Override
        public void finish()
        {
            try
            {
                delegate.finish();
            }
            finally
            {
                ORIGINAL_DIFFICULTIES.forEach((server, difficulty) -> server.setDifficulty(difficulty, true));
                ORIGINAL_DIFFICULTIES.clear();
            }
        }

        private static void reportAndCleanup(GameTestInfo testInfo, java.util.function.Consumer<GameTestInfo> report)
        {
            try
            {
                report.accept(testInfo);
            }
            finally
            {
                GameTestHelper helper = new GameTestHelper(testInfo);
                try
                {
                    GameTestEntities.cleanupTracked(helper);
                }
                finally
                {
                    GameTestConfig.restoreRuntimeDefaults(helper);
                }
            }
        }
    }
}
