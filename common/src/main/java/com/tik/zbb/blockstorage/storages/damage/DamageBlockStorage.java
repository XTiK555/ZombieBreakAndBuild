package com.tik.zbb.blockstorage.storages.damage;

import com.tik.zbb.Constants;
import com.tik.zbb.ai.action.actions.breakk.BreakAction;
import com.tik.zbb.blockstorage.BaseBlockStorage;
import com.tik.zbb.event.MixinEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.greenrobot.eventbus.Subscribe;

public class DamageBlockStorage extends BaseBlockStorage<DamageBlockStorageEntry>
{
    @Subscribe
    public void onAnyBlockWillBroke(BreakAction.OnAnyBlockWillBrokeEvent event)
    {
        remove(event.level(), event.pos());
    }

    @Subscribe
    public void onLevelChunkBlockChanged(MixinEvents.OnLevelChunkBlockChangedEvent event)
    {
        remove(event.level(), event.pos());
    }

    public int addDamageData(ServerLevel level, BlockPos pos, int addDamage, int blockPosId)
    {
        DamageBlockStorageEntry current = get(level, pos);
        int newDamage = (current == null ? 0 : current.damage()) + addDamage;

        put(level, pos, new DamageBlockStorageEntry(newDamage, level.getGameTime(), blockPosId));
        return newDamage;
    }

    @Override
    protected boolean isExpired(DamageBlockStorageEntry entry, long now, long ttlTicks)
    {
        return now - entry.lastTick() > ttlTicks;
    }

    @Override
    protected void onRemove(ServerLevel level, long posKey, DamageBlockStorageEntry entry)
    {
        BlockPos pos = BlockPos.of(posKey);
        level.destroyBlockProgress(entry.blockPosId(), pos, -1);
        Constants.EVENT_BUS.post(new OnBlockDamageProgressRemovedEvent(level, pos));
    }

    public record OnBlockDamageProgressRemovedEvent(ServerLevel level, BlockPos pos) {}
}
