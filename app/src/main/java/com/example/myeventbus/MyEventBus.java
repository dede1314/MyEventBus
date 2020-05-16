package com.example.myeventbus;

import android.os.AsyncTask;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;

/**
 * @author zhoujishi
 * @description
 * @date 2020/5/15
 */
public class MyEventBus {
    private static final String TAG = "MyEventBus";
    static volatile MyEventBus defaultInstance;
    private static final MyEventBusBuilder DEFAULT_BUILDER = new MyEventBusBuilder();
    private static final Map<Class<?>, List<Class<?>>> eventTypeCache = new HashMap<>();

    private final Map<Class<?>, CopyOnWriteArrayList<Subscription>> subscriptionByEventType;
    private final Map<Object, List<Class<?>>> typesBySubscriber;
    private final Map<Class<?>, Object> stickyEvent;

    private final ThreadLocal<PostingThreadState> currentPostingThreadState = new ThreadLocal<PostingThreadState>() {
        @Nullable
        @Override
        protected PostingThreadState initialValue() {
            return new PostingThreadState();
        }
    };
    private final MainThreadSupport mainThreadSupport;
    private final Poster mainThreadPoster;
    private final BackgroundPoster backgroundPoster;
    private final AsyncPoster asyncPoster;
    private final SubscriberMethodFinder subscriberMethodFinder;
    private final ExecutorService executorService;

    public final boolean logSubscriberExceptions;
    public final boolean logNoSubscriberMessages;
    public final boolean sendSubscriberExceptionEvent;
    public final boolean sendNoSubscriberEvent;
    public final boolean throwsSubscriberException;
    public final boolean eventInheritance;

    private final int indexCount;
    private final Logger logger;

    public static MyEventBus getDefault() {
        MyEventBus instance = defaultInstance;
        if (instance == null) {
            synchronized (MyEventBus.class) {
                instance = MyEventBus.defaultInstance;
                if (instance == null) {
                    instance = MyEventBus.defaultInstance = new MyEventBus();
                }
            }
        }
        return instance;
    }

    public static MyEventBusBuilder builder() {
        return new MyEventBusBuilder();
    }

    public static void clearCache() {
        SubscriberMethodFinder.clearCaches();
        eventTypeCache.clear();
    }

    public MyEventBus() {
        this(DEFAULT_BUILDER);
    }

    public MyEventBus(MyEventBusBuilder builder) {
        logger = builder().logger;
        subscriptionByEventType = new HashMap<>();
        typesBySubscriber = new HashMap<>();
        stickyEvent = new ConcurrentHashMap<>();
        mainThreadSupport = builder.getMainThreadSupport();
        mainThreadPoster = mainThreadSupport != null ? mainThreadSupport.createPoster(this) : null;
        backgroundPoster = new BackgroundPoster(this);
        asyncPoster = new AsyncPoster(this);
        indexCount = builder.subscriberInfoIndexes != null ? builder.subscriberInfoIndexes.size() : 0;
        subscriberMethodFinder = new SubscriberMethodFinder(builder.subscriberInfoIndexes,
                builder.strictMethodVerification,builder.ignoreGenerateIndex);// TODO
        logSubscriberExceptions = builder.logSubscriberExceptions;
        logNoSubscriberMessages = builder.logNoSubscriberMessages;
        sendSubscriberExceptionEvent = builder.sendSubscriberExceptionEvent;
        sendNoSubscriberEvent = builder.sendNoSubscriberEvent;
        throwsSubscriberException = builder.throwsSubscriberException;
        eventInheritance = builder.eventInheritance;
        executorService=builder.executorService;
    }

    public void invokeSubscriber(PendingPost pendingPost) {
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public Logger getLogger() {
        return logger;
    }


    final static class PostingThreadState {
        final List<Object> eventQueue = new ArrayList<>();
        boolean isPosting;
        boolean isMainThread;
        Subscription subscription;
        Object Event;
        boolean canceled;
    }
}
