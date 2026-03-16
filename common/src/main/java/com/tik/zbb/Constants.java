package com.tik.zbb;

import org.greenrobot.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Constants
{
    public static final String MOD_ID = "zbb";
    public static final String MOD_NAME = "Zombies Break & Build";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);
    public static final EventBus EVENT_BUS = new EventBus();
}