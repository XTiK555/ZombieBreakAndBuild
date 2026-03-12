package com.tik.zbb;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.WeakHashMap;
import java.util.function.LongConsumer;

public final class BlockStorage
{
    private static final WeakHashMap<ServerLevel, Long2IntOpenHashMap> damageByPosMap = new WeakHashMap<>();
    private static final WeakHashMap<ServerLevel, Long2LongOpenHashMap> lastDamageTickByPosMap = new WeakHashMap<>();

    private static final WeakHashMap<ServerLevel, Long2IntOpenHashMap> buildsByPosMap = new WeakHashMap<>();
    private static final WeakHashMap<ServerLevel, Long2LongOpenHashMap> builtTickByPosMap = new WeakHashMap<>();

    public static void addBuild(ServerLevel level, BlockPos pos)
    {
        long key = pos.asLong();
        Long2IntOpenHashMap buildMap = buildsByPosMap.computeIfAbsent(level, l -> new Long2IntOpenHashMap());
        Long2LongOpenHashMap whenMap = builtTickByPosMap.computeIfAbsent(level, l -> new Long2LongOpenHashMap());

        int now = buildMap.getOrDefault(key, 0) + 1;
        buildMap.put(key, now);
        whenMap.put(key, level.getGameTime());
    }

    public static void removeBuildData(ServerLevel level, BlockPos pos)
    {
        long key = pos.asLong();
        var buildMap = buildsByPosMap.get(level);
        var builtTimeMap = builtTickByPosMap.get(level);

        if (buildMap != null) buildMap.remove(key);
        if (builtTimeMap != null) builtTimeMap.remove(key);
    }

    public static boolean buildMapContains(ServerLevel level, BlockPos pos)
    {
        long key = pos.asLong();
        Long2IntOpenHashMap buildMap = buildsByPosMap.get(level);
        if (buildMap == null) return false;

        if (!buildMap.containsKey(key)) return false;

        Long2LongOpenHashMap whenMap = builtTickByPosMap.get(level);
        if (whenMap == null || !whenMap.containsKey(key))
        {
            buildMap.remove(key);
            return false;
        }

        return true;
    }

    public static void cleanUpBuildData(ServerLevel level, long ttlTicks)
    {
        cleanup(level, buildsByPosMap, builtTickByPosMap, ttlTicks, null);
    }

    public static int addDamage(ServerLevel level, BlockPos pos, int addDamage)
    {
        long key = pos.asLong();
        Long2IntOpenHashMap map = damageByPosMap.computeIfAbsent(level, l -> new Long2IntOpenHashMap());
        Long2LongOpenHashMap last = lastDamageTickByPosMap.computeIfAbsent(level, l -> new Long2LongOpenHashMap());

        int damageNow = map.getOrDefault(key, 0) + addDamage;
        map.put(key, damageNow);
        last.put(key, level.getGameTime());
        return damageNow;
    }

    public static void removeDamageData(ServerLevel level, BlockPos pos)
    {
        long key = pos.asLong();
        var map = damageByPosMap.get(level);
        var last = lastDamageTickByPosMap.get(level);
        if (map != null) map.remove(key);
        if (last != null) last.remove(key);
        level.destroyBlockProgress(pos.hashCode(), pos, -1);
    }

    public static void cleanUpDamageData(ServerLevel level, long ttlTicks)
    {
        cleanup(level, damageByPosMap, lastDamageTickByPosMap, ttlTicks, key ->
        {
            BlockPos pos = BlockPos.of(key);
            level.destroyBlockProgress(pos.hashCode(), pos, -1);
        });
    }

    private static void cleanup(ServerLevel level, WeakHashMap<ServerLevel, Long2IntOpenHashMap> valueMap, WeakHashMap<ServerLevel, Long2LongOpenHashMap> timeMap, long ttlTicks, LongConsumer onExpire)
    {
        Long2IntOpenHashMap values = valueMap.get(level);
        Long2LongOpenHashMap times = timeMap.get(level);
        if (values == null || times == null) return;

        long now = level.getGameTime();

        ObjectIterator<Long2LongMap.Entry> it = times.long2LongEntrySet().fastIterator();
        while (it.hasNext())
        {
            Long2LongMap.Entry e = it.next();
            long key = e.getLongKey();
            long t = e.getLongValue();

            if (now - t > ttlTicks)
            {
                it.remove();
                values.remove(key);
                if (onExpire != null) onExpire.accept(key);
            }
        }
    }
}
