package com.tik.zbb.config.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface ResourceLocationSemantics
{
    ResourceLocationRegistry key() default ResourceLocationRegistry.NONE;

    ResourceLocationRegistry value() default ResourceLocationRegistry.NONE;

    ResourceLocationRegistry element() default ResourceLocationRegistry.NONE;
}
