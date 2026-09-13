package com.tik.zbb.gametest.fixture;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Map;

public final class WorldGameTest
{
    private WorldGameTest() {}

    public static void floor(GameTestHelper helper, int minX, int maxX, int minZ, int maxZ, int y)
    {
        for (int x = minX; x <= maxX; x++)
            for (int z = minZ; z <= maxZ; z++) helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
    }

    public static void corridor(GameTestHelper helper, int minX, int maxX, int centerZ, int floorY)
    {
        floor(helper, minX, maxX, centerZ - 1, centerZ + 1, floorY);
        corridorWalls(helper, minX, maxX, centerZ, floorY);
    }

    public static void corridorWalls(GameTestHelper helper, int minX, int maxX, int centerZ, int floorY)
    {
        for (int x = minX; x <= maxX; x++)
            for (int y = floorY + 1; y <= floorY + 3; y++)
            {
                helper.setBlock(new BlockPos(x, y, centerZ - 2), Blocks.OBSIDIAN);
                helper.setBlock(new BlockPos(x, y, centerZ + 2), Blocks.OBSIDIAN);
            }
    }

    public static GapLane twoBlockGapLane(GameTestHelper helper, int centerZ, int floorY)
    {
        floor(helper, 0, 1, centerZ - 1, centerZ + 1, floorY);
        floor(helper, 4, 6, centerZ - 1, centerZ + 1, floorY);
        corridorWalls(helper, 0, 6, centerZ, floorY);
        for (int x = 0; x <= 6; x++)
            for (int y = floorY + 1; y <= floorY + 3; y++)
            {
                helper.setBlock(new BlockPos(x, y, centerZ - 1), Blocks.OBSIDIAN);
                helper.setBlock(new BlockPos(x, y, centerZ + 1), Blocks.OBSIDIAN);
            }
        for (int x = 2; x <= 3; x++)
            for (int y = 0; y <= floorY; y++)
                for (int z = centerZ - 1; z <= centerZ + 1; z++) helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
        return new GapLane(new BlockPos(1, floorY + 1, centerZ), new BlockPos(5, floorY + 1, centerZ),
                new BlockPos(2, floorY, centerZ), new BlockPos(3, floorY, centerZ));
    }

    public static void solidRay(GameTestHelper helper, int startX, int blocks, int y, int z)
    {
        for (int x = startX; x < startX + blocks; x++) helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
    }

    public static void column(GameTestHelper helper, int x, int minY, int maxY, int z, Block block)
    {
        for (int y = minY; y <= maxY; y++) helper.setBlock(new BlockPos(x, y, z), block);
    }

    public static void barrier(GameTestHelper helper, int x, int centerZ, Block center)
    {
        for (int y = 2; y <= 4; y++)
            for (int z = centerZ - 1; z <= centerZ + 1; z++)
                helper.setBlock(new BlockPos(x, y, z), y <= 3 && z == centerZ ? center : Blocks.OBSIDIAN);
    }

    public static BarrierLane barrierLane(GameTestHelper helper, int centerZ, Block barrier)
    {
        corridor(helper, 0, 6, centerZ, 1);
        barrier(helper, 3, centerZ, barrier);
        return new BarrierLane(new BlockPos(1, 2, centerZ), new BlockPos(5, 2, centerZ),
                new BlockPos(3, 2, centerZ), new BlockPos(3, 3, centerZ));
    }

    public static boolean barrierIs(GameTestHelper helper, int x, int centerZ, Block block)
    {
        return state(helper, new BlockPos(x, 2, centerZ)).is(block)
                && state(helper, new BlockPos(x, 3, centerZ)).is(block);
    }

    public static boolean barrierIsAir(GameTestHelper helper, int x, int centerZ)
    {
        return barrierIs(helper, x, centerZ, Blocks.AIR);
    }

    public static boolean barrierHasAir(GameTestHelper helper, int x, int centerZ)
    {
        return state(helper, new BlockPos(x, 2, centerZ)).isAir()
                || state(helper, new BlockPos(x, 3, centerZ)).isAir();
    }

    public static BlockState state(GameTestHelper helper, BlockPos relativePos)
    {
        return helper.getLevel().getBlockState(helper.absolutePos(relativePos));
    }

    public record GapLane(BlockPos mobStart, BlockPos target, BlockPos firstGap, BlockPos secondGap) {}

    public record BarrierLane(BlockPos mobStart, BlockPos target, BlockPos lowerBarrier, BlockPos upperBarrier)
    {
        public boolean is(GameTestHelper helper, Block block)
        {
            return state(helper, lowerBarrier).is(block) && state(helper, upperBarrier).is(block);
        }

        public boolean hasAir(GameTestHelper helper)
        {
            return state(helper, lowerBarrier).isAir() || state(helper, upperBarrier).isAir();
        }
    }

    public static Map<BlockPos, BlockState> snapshot(GameTestHelper helper, int minX, int maxX,
                                                     int minY, int maxY, int minZ, int maxZ)
    {
        Map<BlockPos, BlockState> states = new java.util.HashMap<>();
        for (int x = minX; x <= maxX; x++)
            for (int y = minY; y <= maxY; y++)
                for (int z = minZ; z <= maxZ; z++)
                {
                    BlockPos pos = new BlockPos(x, y, z);
                    states.put(pos, state(helper, pos));
                }
        return states;
    }

    public static void assertUnchanged(GameTestHelper helper, Map<BlockPos, BlockState> before)
    {
        for (Map.Entry<BlockPos, BlockState> entry : before.entrySet())
            helper.assertTrue(state(helper, entry.getKey()).equals(entry.getValue()),
                    "Mob unexpectedly modified the world at " + entry.getKey());
    }

    public static List<ItemEntity> itemsNear(GameTestHelper helper, BlockPos absolutePos)
    {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(absolutePos).inflate(3));
    }

}
