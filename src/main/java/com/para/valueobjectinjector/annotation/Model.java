package com.para.valueobjectinjector.annotation;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Model {
    Class<?> targetClass();

    String TARGET_CLASS = "targetClass";
}
