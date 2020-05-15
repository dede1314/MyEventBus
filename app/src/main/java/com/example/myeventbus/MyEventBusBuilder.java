package com.example.myeventbus;


import android.os.Looper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author zhoujishi
 * @description
 * @date 2020/5/15
 */
public class MyEventBusBuilder {
    private final static ExecutorService DEFAULT_EXECUTOR_SERVICE = Executors.newCachedThreadPool();
    boolean logSubscriberExceptions = true;
    boolean logNoSubscriberMessages = true;
    boolean sendSubscriberExceptionEvent = true;
    boolean sendNoSubscriberEvent = true;
    boolean throwsSubscriberException;
    boolean eventInheritance = true;
    boolean ignoreGenerateIndex;
    boolean strictMethodVerification;
    ExecutorService executorService = DEFAULT_EXECUTOR_SERVICE;
    List<Class<?>> skipMethodVerificationForClass;
    List<SubscriberInfoIndex> subscriberInfoIndexes;
    Logger logger;
    MainThreadSupport mainThreadSupport;

    public MyEventBusBuilder() {
    }

    public MyEventBusBuilder logSubscriberxceptions(boolean logSubscriberExceptions) {
        this.logSubscriberExceptions = logSubscriberExceptions;
        return this;
    }

    public MyEventBusBuilder logNoSubscriberMessage(boolean logNoSubscriberMessages) {
        this.logNoSubscriberMessages = logNoSubscriberMessages;
        return this;
    }

    public MyEventBusBuilder sendSubscriberExceptionEvent(boolean sendSubscriberExceptionEvent) {
        this.sendSubscriberExceptionEvent = logNoSubscriberMessages;
        return this;
    }

    public MyEventBusBuilder sendNoSubscriberEvent(boolean sendNoSubscriberEvent) {
        this.sendNoSubscriberEvent = sendNoSubscriberEvent;
        return this;
    }

    public MyEventBusBuilder throwsSubscriberException(boolean throwsSubscriberException) {
        this.throwsSubscriberException = throwsSubscriberException;
        return this;
    }

    public MyEventBusBuilder eventInheritance(boolean eventInheritance) {
        this.eventInheritance = eventInheritance;
        return this;
    }

    public MyEventBusBuilder executorService(ExecutorService executorService) {
        this.executorService = executorService;
        return this;
    }

    public MyEventBusBuilder skipMethodVerificationFor(Class<?> clazz) {
        if (skipMethodVerificationForClass == null) {
            skipMethodVerificationForClass = new ArrayList<>();
        }
        skipMethodVerificationForClass.add(clazz);
        return this;
    }

    public MyEventBusBuilder ignoreGenerateIndex(boolean ignoreGenerateIndex) {
        this.ignoreGenerateIndex = ignoreGenerateIndex;
        return this;
    }

    public MyEventBusBuilder strictMethodVerification(boolean strictMethodVerification) {
        this.strictMethodVerification = strictMethodVerification;
        return this;
    }

    public MyEventBusBuilder addIndex(SubscriberInfoIndex infoIndex) {
        if (subscriberInfoIndexes == null) {
            subscriberInfoIndexes = new ArrayList<>();
        }
        subscriberInfoIndexes.add(infoIndex);
        return this;
    }

    public MyEventBusBuilder logger(Logger logger) {
        this.logger = logger;
        return this;
    }

    Logger getLogger() {
        if (logger != null) {
            return logger;
        } else {
            return Logger.Default.get();
        }
    }

    MainThreadSupport getMainThreadSupport() {
        if (mainThreadSupport != null) {
            return mainThreadSupport;
        } else if (AndroidLogger.isAndroidLogAvailable()) {
            Object looperOrNull = getAndroidMainLooperOrNull();
            return looperOrNull == null ? null : new MainThreadSupport.AndroidHandlerMainThreadSupport((Looper) looperOrNull);
        } else {
            return null;
        }
    }

    static Object getAndroidMainLooperOrNull() {
        try {
            return Looper.getMainLooper();
        } catch (RuntimeException e) {
            return null;
        }
    }

    public MyEventBus installDefaultEvent() {
        synchronized (MyEventBus.class) {
            if (MyEventBus.defaultInstance != null) {
                throw new EventBusException("Default instance already exists. "+
                        "  It may be only set once before it's used the first time to ensure consistent behavior.");
            }
            MyEventBus.defaultInstance = build();
            return MyEventBus.defaultInstance;
        }
    }

    public MyEventBus build(){
        return new MyEventBus(this);
    }

}
