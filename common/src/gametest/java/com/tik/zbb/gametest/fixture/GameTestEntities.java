package com.tik.zbb.gametest.fixture;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class GameTestEntities
{
    private static final AtomicInteger PLAYER_NUMBER = new AtomicInteger();
    private static final Map<TestRun, Set<Entity>> TRACKED_ENTITIES = new HashMap<>();

    private GameTestEntities() {}

    public static Zombie frozenZombie(GameTestHelper helper, BlockPos pos)
    {
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, Vec3.atBottomCenterOf(pos));
        zombie.setNoAi(true);
        zombie.setInvulnerable(true);
        return track(helper, zombie);
    }

    public static Zombie zombie(GameTestHelper helper, BlockPos pos)
    {
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, Vec3.atBottomCenterOf(pos));
        zombie.getRandom().setSeed(0L);
        zombie.setBaby(false);
        zombie.setPersistenceRequired();
        zombie.setInvulnerable(true);
        return track(helper, zombie);
    }

    public static Zombie stationaryZombie(GameTestHelper helper, BlockPos pos)
    {
        Zombie zombie = zombie(helper, pos);
        zombie.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.0);
        return zombie;
    }

    public static Zombie shortSightedStationaryZombie(GameTestHelper helper, BlockPos pos, double followRange)
    {
        Zombie zombie = GameTestEntities.stationaryZombie(helper, pos);
        zombie.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(followRange);
        return zombie;
    }

    public static void keepTargeting(GameTestHelper helper, Mob mob, LivingEntity target)
    {
        mob.setTarget(target);
        helper.onEachTick(() ->
        {
            if (mob.isRemoved() || target.isRemoved() || !mob.isAlive() || !target.isAlive()) return;

            mob.setTarget(target);
        });
    }

    public static Villager frozenVillager(GameTestHelper helper, BlockPos pos)
    {
        Villager villager = helper.spawn(EntityType.VILLAGER, Vec3.atBottomCenterOf(pos));
        villager.setNoAi(true);
        villager.setNoGravity(true);
        keepAlive(villager);
        return track(helper, villager);
    }

    public static Villager vulnerableFrozenVillager(GameTestHelper helper, BlockPos pos)
    {
        Villager villager = helper.spawn(EntityType.VILLAGER, Vec3.atBottomCenterOf(pos));
        villager.setNoAi(true);
        return track(helper, villager);
    }

    public static ServerPlayer serverPlayer(GameTestHelper helper, BlockPos relativePos, GameType gameType)
    {
        GameProfile profile = new GameProfile(UUID.randomUUID(), "zbb-test-" + PLAYER_NUMBER.incrementAndGet());
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), profile,
                ClientInformation.createDefault());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        BlockPos pos = helper.absolutePos(relativePos);
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setGameMode(gameType);
        if (gameType == GameType.SURVIVAL) keepAlive(player);
        return track(helper, player);
    }

    public static void cleanupTracked(GameTestHelper helper)
    {
        Set<Entity> tracked = TRACKED_ENTITIES.remove(testRun(helper));
        if (tracked == null) return;

        for (Entity entity : tracked)
        {
            if (entity instanceof ServerPlayer player)
            {
                if (!player.isRemoved())
                {
                    helper.getLevel().getServer().getPlayerList().remove(player);
                }
            }
            else if (!entity.isRemoved())
            {
                entity.discard();
            }
        }
    }

    public static void removeServerPlayer(GameTestHelper helper, ServerPlayer player)
    {
        if (player == null) return;

        TestRun testRun = testRun(helper);
        Set<Entity> tracked = TRACKED_ENTITIES.get(testRun);
        if (tracked != null)
        {
            tracked.remove(player);
            if (tracked.isEmpty()) TRACKED_ENTITIES.remove(testRun);
        }

        if (!player.isRemoved())
        {
            helper.getLevel().getServer().getPlayerList().remove(player);
        }
    }

    private static <T extends Entity> T track(GameTestHelper helper, T entity)
    {
        TRACKED_ENTITIES.computeIfAbsent(testRun(helper),
                ignored -> Collections.newSetFromMap(new IdentityHashMap<>())).add(entity);
        return entity;
    }

    private static TestRun testRun(GameTestHelper helper)
    {
        return new TestRun(helper.getLevel(), helper.absolutePos(BlockPos.ZERO));
    }

    private static void keepAlive(net.minecraft.world.entity.LivingEntity entity)
    {
        entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20_000, 4, false, false));
        entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20_000, 4, false, false));
    }

    private record TestRun(ServerLevel level, BlockPos origin) {}
}
