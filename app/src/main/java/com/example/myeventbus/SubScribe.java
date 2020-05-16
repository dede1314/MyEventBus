package com.example.myeventbus;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author zhoujishi
 * @description
 * @date 2020/5/16
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SubScribe {
    ThreadMode threadMode() default ThreadMode.POSTING;
    boolean sticky() default false;
    int priority() default 0;
}
