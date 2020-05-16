package com.example.myeventbus;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zhoujishi
 * @description
 * @date 2020/5/15
 */
public class SubscriberMethodFinder {
    private static final int BRIDGE = 0x40;
    private static final int SYNTHETIC = 0x1000;

    private static final int MODIFIES_IGNORES = Modifier.ABSTRACT | Modifier.STATIC | BRIDGE | SYNTHETIC;
    private static final Map<Class<?>, List<SubscriberMethod>> METHOD_CACHE = new ConcurrentHashMap<>();
    private List<SubscriberInfoIndex> subscriberInfoIndices;
    private final boolean strictMethodVerification;
    private final boolean ignoreGenerateIndex;

    private static final int POOL_SIZE = 4;

    private static final FindState[] FIND_STATE_POOL = new FindState[POOL_SIZE];


    public SubscriberMethodFinder(List<SubscriberInfoIndex> subscriberInfoIndices, boolean strictMethodVerification, boolean ignoreGenerateIndex) {
        this.subscriberInfoIndices = subscriberInfoIndices;
        this.strictMethodVerification = strictMethodVerification;
        this.ignoreGenerateIndex = ignoreGenerateIndex;
    }


    List<SubscriberMethod> findSubscriberMethods(Class<?> subscriberClass) {
        List<SubscriberMethod> subscriberMethods = METHOD_CACHE.get(subscriberClass);
        if (subscriberMethods != null) {
            return subscriberMethods;
        }
        if (ignoreGenerateIndex) {
            subscriberClass = findUseingReflection(subscriberClass);
        } else {
            subscriberClass = findUseingInfo(subscriberClass);
        }
        if (subscriberMethods.isEmpty()) {
            throw new EventBusException("Subscriber " + subscriberClass + "and its super class has no public method with the @Subscriber annotation");
        } else {
            METHOD_CACHE.put(subscriberClass, subscriberMethods);
            return subscriberMethods;
        }
    }

    private Class<?> findUseingInfo(Class<?> subscriberClass) {
    }

    private Class<?> findUseingReflection(Class<?> subscriberClass) {
    }

    static void clearCaches() {

    }


    static class FindState {
    }
}
