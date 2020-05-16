package com.example.myeventbus;


/**
 * @author zhoujishi
 * @description
 * @date 2020/5/16
 */
public interface SubscriberInfo {
    Class<?> getSubscriberClass();
    SubscriberMethod[] getSubscriberMethods();
    SubscriberInfo getSuperSubscriberInfo();
    boolean shouldCheckSuperClass();
}
