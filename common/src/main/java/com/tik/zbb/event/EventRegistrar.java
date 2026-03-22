package com.tik.zbb.event;

import com.tik.zbb.Constants;
import com.tik.zbb.ai.action.actions.breakk.BreakVisual;
import com.tik.zbb.ai.action.actions.build.BuildVisual;
import com.tik.zbb.blockstorage.BlockStorages;
import com.tik.zbb.blockstorage.storages.broken.BrokenReappearBlockVisual;
import com.tik.zbb.blockstorage.storages.buildDisappear.BuildDisappearBlockVisual;

public final class EventRegistrar
{
    public static void registerAll()
    {
        // storage managers
        Constants.EVENT_BUS.register(BlockStorages.DAMAGE_MANAGER);
        Constants.EVENT_BUS.register(BlockStorages.BUILD_PROTECTION_MANAGER);
        Constants.EVENT_BUS.register(BlockStorages.BUILD_DISAPPEAR_MANAGER);
        Constants.EVENT_BUS.register(BlockStorages.BROKEN_MANAGER);
        Constants.EVENT_BUS.register(BlockStorages.ID_MANAGER);

        // storage visuals
        Constants.EVENT_BUS.register(new BrokenReappearBlockVisual());
        Constants.EVENT_BUS.register(new BuildDisappearBlockVisual());


        // action visuals
        Constants.EVENT_BUS.register(new BreakVisual());
        Constants.EVENT_BUS.register(new BuildVisual());
    }
}
