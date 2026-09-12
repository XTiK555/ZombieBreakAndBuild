package com.tik.zbb.gametest.capability;

import com.tik.zbb.Constants;
import com.tik.zbb.gametest.fixture.GameTestConfig;
import com.tik.zbb.gametest.fixture.GameTestEntities;
import com.tik.zbb.gametest.fixture.WorldGameTest;
import com.tik.zbb.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.GameType;

import java.util.Set;

public final class PlatformWiringGameTests
{
    private PlatformWiringGameTests() {}

    public static void platformAndLoaderWiringSmoke(GameTestHelper helper)
    {
        String platform = Services.PLATFORM.getPlatformName();
        helper.assertTrue(Set.of("Fabric", "Forge", "NeoForge").contains(platform),
                "Unexpected platform service implementation: " + platform);
        helper.assertTrue(Services.PLATFORM.isModLoaded(Constants.MOD_ID),
                platform + " platform service did not find the loaded ZBB mod");
        helper.assertTrue(Services.PLATFORM.isDevelopmentEnvironment(),
                platform + " GameTests are not running in a development environment");
        helper.succeed();
    }

    public static void realConfigCommandHotReloadsExistingMobBehavior(GameTestHelper helper)
    {
        GameTestConfig.accelerateActions(helper);
        GameTestConfig.add(helper, "ai.ignoreBuildEntityIdList", "minecraft:zombie");
        WorldGameTest.GapLane lane = WorldGameTest.twoBlockGapLane(helper, 2, 3);
        ServerPlayer player = GameTestEntities.serverPlayer(helper, lane.target(), GameType.SURVIVAL);
        Zombie zombie = GameTestEntities.zombie(helper, lane.mobStart());
        GameTestEntities.keepTargeting(helper, zombie, player);
        double movementSpeed = zombie.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue();
        zombie.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0);
        BlockPos bridgeFirst = lane.firstGap();
        BlockPos bridgeSecond = lane.secondGap();

        helper.runAtTickTime(35, () ->
        {
            helper.assertTrue(
                    WorldGameTest.state(helper, bridgeFirst).isAir() && WorldGameTest.state(helper, bridgeSecond).isAir(),
                    "The excluded zombie built before the runtime config change"
            );

            GameTestConfig.runConfigCommandExpectingChange(helper, "reset ai.ignoreBuildEntityIdList runtime_only");
            zombie.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(movementSpeed);
            zombie.setTarget(player);
        });
        helper.succeedWhen(() ->
        {
            helper.assertTrue(
                    zombie.isAlive(),
                    "The original zombie was replaced or died"
            );
            helper.assertTrue(
                    !WorldGameTest.state(helper, bridgeFirst).isAir() || !WorldGameTest.state(helper, bridgeSecond).isAir(),
                    "The same zombie did not adopt the runtime build setting"
            );
        });
    }

}
