package com.example.myeventbus;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
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
    private List<SubscriberInfoIndex> subscriberInfoIndexes;
    private final boolean strictMethodVerification;
    private final boolean ignoreGenerateIndex;

    private static final int POOL_SIZE = 4;

    private static final FindState[] FIND_STATE_POOL = new FindState[POOL_SIZE];


    public SubscriberMethodFinder(List<SubscriberInfoIndex> subscriberInfoIndices, boolean strictMethodVerification, boolean ignoreGenerateIndex) {
        this.subscriberInfoIndexes = subscriberInfoIndices;
        this.strictMethodVerification = strictMethodVerification;
        this.ignoreGenerateIndex = ignoreGenerateIndex;
    }


    List<SubscriberMethod> findSubscriberMethods(Class<?> subscriberClass) {
        List<SubscriberMethod> subscriberMethods = METHOD_CACHE.get(subscriberClass);
        if (subscriberMethods != null) {
            return subscriberMethods;
        }
        if (ignoreGenerateIndex) {
            subscriberMethods = findUsingReflection(subscriberClass);
        } else {
            subscriberMethods = findUseingInfo(subscriberClass);
        }
        if (subscriberMethods.isEmpty()) {
            throw new EventBusException("Subscriber " + subscriberClass + "and its super class has no public method with the @Subscriber annotation");
        } else {
            METHOD_CACHE.put(subscriberClass, subscriberMethods);
            return subscriberMethods;
        }
    }

    private List<SubscriberMethod> findUseingInfo(Class<?> subscriberClass) {
        FindState findState = prepareFindState();
        findState.initForSubscriber(subscriberClass);
        while (findState.clazz != null) {
            findState.subscriberInfo = getSubscriberInfo(findState);
            if (findState.subscriberInfo != null) {
                SubscriberMethod[] array = findState.subscriberInfo.getSubscriberMethods();
                for (SubscriberMethod subscriberMethod : array) {
                    if (findState.checkAdd(subscriberMethod.method, subscriberMethod.eventType)) {
                        findState.subscriberMethods.add(subscriberMethod);
                    }

                }
            } else {
                findUsingReflectionInSingleClass(findState);
            }
            findState.moveToSuperClass();
        }
        return getMethodAndRelease(findState);
    }

    private List<SubscriberMethod> getMethodAndRelease(FindState findState) {
        List<SubscriberMethod> subscriberMethods = new ArrayList<>(findState.subscriberMethods);
        findState.recycle();
        synchronized (FIND_STATE_POOL) {
            for (int i = 0; i < POOL_SIZE; i++) {
                if (FIND_STATE_POOL[i] == null) {
                    FIND_STATE_POOL[i] = findState;
                    break;
                }
            }
        }
        return subscriberMethods;
    }


    private FindState prepareFindState() {
        synchronized (FIND_STATE_POOL) {
            for (int i = 0; i < POOL_SIZE; i++) {
                FindState findState = FIND_STATE_POOL[i];
                if (findState != null) {
                    FIND_STATE_POOL[i] = null;
                    return findState;
                }
            }
        }
        return new FindState();
    }

    private List<SubscriberMethod> findUsingReflection(Class<?> subscriberClass) {
        FindState findState = prepareFindState();
        findState.initForSubscriber(subscriberClass);
        while (findState.clazz != null) {
            findUsingReflectionInSingleClass(findState);
            findState.moveToSuperClass();
        }
        return getMethodAndRelease(findState);
    }


    private void findUsingReflectionInSingleClass(FindState findState) {
        Method[] methods;
        try {
            methods = findState.clazz.getDeclaredMethods();
        } catch (Throwable throwable) {
            try {
                methods = findState.clazz.getMethods();
            } catch (LinkageError error) {
                String msg = "could not inspect method of " + findState.clazz.getName();
                if (ignoreGenerateIndex) {
                    msg += ".please consider using EventBus annotation processor to avoid reflection";
                } else {
                    msg += ". please make class visible to EventBus annotation processor to avoid reflection";
                }
                throw new EventBusException(msg, error);
            }
            findState.skipSuperClass = true;
        }
        for (Method method : methods) {
            int modifiers = method.getModifiers();
            if ((modifiers & Modifier.PUBLIC) != 0 && (modifiers & MODIFIES_IGNORES) == 0) {
                Class<?>[] paramsType = method.getParameterTypes();
                if (paramsType.length == 1) {
                    SubScribe subScribeAntotation = method.getAnnotation(SubScribe.class);
                    if (subScribeAntotation != null) {
                        Class<?> eventType = paramsType[0];
                        if (findState.checkAdd(method, eventType)) {
                            ThreadMode threadMode = subScribeAntotation.threadMode();
                            findState.subscriberMethods.add(new SubscriberMethod(method, threadMode, eventType, subScribeAntotation.priority(),
                                    subScribeAntotation.sticky()));
                        }
                    }
                } else if (strictMethodVerification && method.isAnnotationPresent(SubScribe.class)) {
                    String methodName = method.getDeclaringClass().getName() + "." + method.getName();
                    throw new EventBusException("@Subscriber method " + methodName + "" +
                            "must have exactly 1 params,but has " + paramsType.length);
                }
            } else if (strictMethodVerification && method.isAnnotationPresent(SubScribe.class)) {
                String methodName = method.getDeclaringClass().getName() + "." + method.getName();
                throw new EventBusException(methodName +
                        " is a illegal @SubScribe method:must be public ,non-static,and non-abstract");
            }
        }
    }

    private SubscriberInfo getSubscriberInfo(FindState findState) {
        if (findState.subscriberInfo != null && findState.subscriberInfo.getSuperSubscriberInfo() != null) {
            SubscriberInfo superClassInfo = findState.subscriberInfo.getSuperSubscriberInfo();
            if (findState.clazz == superClassInfo.getSubscriberClass()) {
                return superClassInfo;
            }
        }
        if (subscriberInfoIndexes != null) {
            for (SubscriberInfoIndex subscriberInfoIndex : subscriberInfoIndexes) {
                SubscriberInfo info = subscriberInfoIndex.getSubscirberInfo(findState.clazz);
                if (info != null) {
                    return info;
                }
            }
        }
        return null;
    }

    static void clearCaches() {
        METHOD_CACHE.clear();
    }


    static class FindState {
        final List<SubscriberMethod> subscriberMethods = new ArrayList<>();
        final Map<Class, Object> anyMethodByEventType = new HashMap<>();
        final Map<String, Class> subscriberMethodByMethodKey = new HashMap<>();
        final StringBuilder methodKeyBuilder = new StringBuilder(64);

        Class<?> subscriberClass;
        Class<?> clazz;
        boolean skipSuperClass;
        SubscriberInfo subscriberInfo;

        void initForSubscriber(Class<?> subscriberClass) {
            this.subscriberClass = clazz = subscriberClass;
            skipSuperClass = false;
            subscriberInfo = null;
        }

        void recycle() {
            subscriberMethods.clear();
            anyMethodByEventType.clear();
            subscriberMethodByMethodKey.clear();
            methodKeyBuilder.setLength(0);
            subscriberClass = null;
            clazz = null;
            skipSuperClass = false;
            subscriberInfo = null;
        }

        boolean checkAdd(Method method, Class<?> eventType) {
            Object existing = anyMethodByEventType.put(eventType, method);
            if (existing == null) {
                return true;
            } else {
                if (existing instanceof Method) {
                    if (!checkAddWithMethodSignature((Method) existing, eventType)) {
                        anyMethodByEventType.put(eventType, this);
                    }
                }
                return checkAddWithMethodSignature(method, eventType);
            }
        }

        private boolean checkAddWithMethodSignature(Method method, Class<?> eventType) {
            methodKeyBuilder.setLength(0);
            methodKeyBuilder.append(method.getName());
            methodKeyBuilder.append('>').append(eventType.getName());

            String methodKey = methodKeyBuilder.toString();
            Class<?> methodClass = method.getDeclaringClass();
            Class<?> methodClassOld = subscriberMethodByMethodKey.put(methodKey, methodClass);
            if (methodClassOld == null || methodClassOld.isAssignableFrom(methodClass)) {
                return true;
            } else {
                subscriberMethodByMethodKey.put(methodKey, methodClassOld);
                return false;
            }
        }


        void moveToSuperClass() {
            if (skipSuperClass) {
                clazz = null;
            } else {
                clazz = clazz.getSuperclass();
                String className = clazz.getName();
                if (className.startsWith("java.") || className.startsWith("javax") ||
                        className.startsWith("android") || className.startsWith("androidx")) {
                    clazz = null;
                }
            }
        }

    }

}
