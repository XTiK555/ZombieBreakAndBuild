package com.tik.zbb.event;

import com.tik.zbb.Constants;
import com.tik.zbb.ai.action.actions.breakk.BreakVisual;
import com.tik.zbb.ai.action.actions.build.BuildVisual;
import com.tik.zbb.blockstorage.BlockStorages;
import com.tik.zbb.blockstorage.FallingBlockStorageTracker;
import com.tik.zbb.blockstorage.storages.broken.BrokenReappearBlockStorageVisual;
import com.tik.zbb.blockstorage.storages.buildDisappear.BuildDisappearBlockStorageVisual;
import com.tik.zbb.blockstorage.storages.damage.DamageBlockStorageVisual;

public final class EventRegistrar
{
    public static void registerAll()
    {
        // storage managers
        Constants.EVENT_BUS.register(BlockStorages.DAMAGE_MANAGER);
        Constants.EVENT_BUS.register(BlockStorages.BUILD_PROTECTION_MANAGER);
        Constants.EVENT_BUS.register(BlockStorages.BUILD_DISAPPEAR_MANAGER);
        Constants.EVENT_BUS.register(BlockStorages.BROKEN_MANAGER);

        // optional storage adapters
        Constants.EVENT_BUS.register(new FallingBlockStorageTracker());

        // storage visuals
        Constants.EVENT_BUS.register(new BrokenReappearBlockStorageVisual());
        Constants.EVENT_BUS.register(new BuildDisappearBlockStorageVisual());
        Constants.EVENT_BUS.register(new DamageBlockStorageVisual());


        // action visuals
        Constants.EVENT_BUS.register(new BreakVisual());
        Constants.EVENT_BUS.register(new BuildVisual());
    }
}
