package com.tik.zbb.config.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ResourceLocationSemantics
{
    ResourceLocationRegistry key() default ResourceLocationRegistry.NONE;

    ResourceLocationRegistry value() default ResourceLocationRegistry.NONE;

    ResourceLocationRegistry element() default ResourceLocationRegistry.NONE;
}
