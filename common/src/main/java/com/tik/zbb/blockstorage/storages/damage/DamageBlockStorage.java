package com.tik.zbb.blockstorage.storages.damage;

import com.tik.zbb.blockstorage.BaseBlockStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public class DamageBlockStorage extends BaseBlockStorage<DamageEntry>
{
    public int addDamageData(ServerLevel level, BlockPos pos, int addDamage)
    {
        DamageEntry current = get(level, pos);
        int newDamage = (current == null ? 0 : current.damage()) + addDamage;
        put(level, pos, new DamageEntry(newDamage, level.getGameTime()));
        return newDamage;
    }

    @Override
    protected boolean isExpired(DamageEntry entry, long now, long ttlTicks)
    {
        return now - entry.lastTick() > ttlTicks;
    }
    
    @Override
    protected void onRemove(ServerLevel level, long posKey, DamageEntry entry)
    {
        clearProgress(level, posKey);
    }

    private void clearProgress(ServerLevel level, long posKey)
    {
        BlockPos pos = BlockPos.of(posKey);
        level.destroyBlockProgress(pos.hashCode(), pos, -1);
    }
}
