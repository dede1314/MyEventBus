package com.example.myeventbus;

import android.os.AsyncTask;

import androidx.annotation.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;

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
    private final Map<Class<?>, Object> stickyEvents;

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
        stickyEvents = new ConcurrentHashMap<>();
        mainThreadSupport = builder.getMainThreadSupport();
        mainThreadPoster = mainThreadSupport != null ? mainThreadSupport.createPoster(this) : null;
        backgroundPoster = new BackgroundPoster(this);
        asyncPoster = new AsyncPoster(this);
        indexCount = builder.subscriberInfoIndexes != null ? builder.subscriberInfoIndexes.size() : 0;
        subscriberMethodFinder = new SubscriberMethodFinder(builder.subscriberInfoIndexes,
                builder.strictMethodVerification, builder.ignoreGenerateIndex);// TODO
        logSubscriberExceptions = builder.logSubscriberExceptions;
        logNoSubscriberMessages = builder.logNoSubscriberMessages;
        sendSubscriberExceptionEvent = builder.sendSubscriberExceptionEvent;
        sendNoSubscriberEvent = builder.sendNoSubscriberEvent;
        throwsSubscriberException = builder.throwsSubscriberException;
        eventInheritance = builder.eventInheritance;
        executorService = builder.executorService;
    }

    public void register(Object subscriber) {
        Class<?> subscriberClass = subscriber.getClass();
        List<SubscriberMethod> subscriberMethods = subscriberMethodFinder.findSubscriberMethods(subscriberClass);
        synchronized (this) {
            for (SubscriberMethod subscriberMethod : subscriberMethods) {
                subscriber(subscriber, subscriberMethod);
            }
        }
    }

    public synchronized void unregister(Object subscriber) {
        List<Class<?>> subscriberType = typesBySubscriber.get(subscriber);
        if (subscriberType != null) {
            for (Class<?> eventType : subscriberType) {
                unsubscribeByEventType(subscriber, eventType);
            }
            typesBySubscriber.remove(subscriber);
        } else {
            logger.log(Level.WARNING, "Subscriber to unregister was not registered before " + subscriber.getClass());
        }
    }

    private void unsubscribeByEventType(Object subscriber, Class<?> eventType) {
        List<Subscription> subscriptions=subscriptionByEventType.get(eventType);
        if(subscriptions!=null){
            int size=subscriptions.size();
            for (int i = 0; i < size; i++) {
                Subscription subscription=subscriptions.get(i);
                if(subscription.subscriber==subscriber){
                    subscription.active=false;
                    subscriptions.remove(i);
                    i--;
                    size--;
                }
            }
        }
    }

    public void cancelEventDelivery(Object object){
        // TODO
    }

    public void postSticky(Object event){
        synchronized (stickyEvents){
            stickyEvents.put(event.getClass(),event);
        }
        post(event);
    }

    public <T> T getStickyEvent(Class<T> eventType){
        synchronized (stickyEvents){
            return eventType.cast(stickyEvents.get(eventType));
        }
    }

    public <T> T removeStickyEvent(Class<T> eventType){
        return eventType.cast(stickyEvents.remove(eventType));
    }

    public boolean removeStickyEvent(Object event){
        synchronized (stickyEvents){
            Class<?>  eventType=event.getClass();
            Object existingEvent=stickyEvents.get(eventType);
            if(event.equals(existingEvent)){
                stickyEvents.remove(eventType);
                return true;
            }else {
                return false;
            }
        }
    }


    private void subscriber(Object subscriber, SubscriberMethod subscriberMethod) {
        final Class<?> eventType = subscriberMethod.eventType;
        Subscription newSubscription = new Subscription(subscriber, subscriberMethod);
        CopyOnWriteArrayList<Subscription> subscriptions = subscriptionByEventType.get(eventType);
        if (subscriptions == null) {
            subscriptions = new CopyOnWriteArrayList<>();
            subscriptionByEventType.put(eventType, subscriptions);
        } else {
            if (subscriptions.contains(newSubscription)) {
                throw new EventBusException("Subscriber " + subscriber + " already registered to event " + eventType);
            }
        }
        int size = subscriptions.size();
        for (int i = 0; i <= size; i++) {
            if (i == size || subscriberMethod.priority > subscriptions.get(i).subscriberMethod.priority) {
                subscriptions.add(i, newSubscription);
                break;
            }
        }
        List<Class<?>> subscriberEvent = typesBySubscriber.get(subscriber);
        if (subscriberEvent == null) {
            subscriberEvent = new ArrayList<>();
            typesBySubscriber.put(subscriber, subscriberEvent);
        }
        subscriberEvent.add(eventType);
        if (subscriberMethod.sticky) {
            if (eventInheritance) {
                Set<Map.Entry<Class<?>, Object>> entries = stickyEvents.entrySet();
                for (Map.Entry<Class<?>, Object> entry : entries) {
                    Class<?> candidateEventType = entry.getKey();
                    if (eventType.isAssignableFrom(candidateEventType)) {
                        Object stickyEvent = entry.getValue();
                        checkPostStickyEventToSubscription(newSubscription, stickyEvent);
                    }
                }
            } else {
                Object stickyEvent = stickyEvents.get(eventType);
                checkPostStickyEventToSubscription(newSubscription, stickyEvent);

            }
        }
    }

    private void checkPostStickyEventToSubscription(Subscription newSubscription, Object stickyEvent) {
        if (stickyEvent != null) {
            postToSubscription(newSubscription, stickyEvent, isMainThread());
        }
    }

    private void postToSubscription(Subscription subscription, Object event, boolean mainThread) {
        switch (subscription.subscriberMethod.threadMode) {
            case POSTING:
                invokeSubscriber(subscription, event);
                break;

            case MAIN:
                if (mainThread) {
                    invokeSubscriber(subscription, event);
                } else {
                    mainThreadPoster.enqueue(subscription, event);
                }
                break;
            case MAIN_ORDERED:
                if (mainThreadPoster != null) {
                    mainThreadPoster.enqueue(subscription, event);
                } else {
                    invokeSubscriber(subscription, event);
                }
                break;
            case BACKGROUND:
                if (mainThread) {
                    backgroundPoster.enqueue(subscription, event);
                } else {
                    invokeSubscriber(subscription, event);
                }
                break;
            case ASYNC:
                asyncPoster.enqueue(subscription, event);
                break;
            default:
                throw new IllegalStateException("unknown thread mode" + subscription.subscriberMethod.threadMode);
        }
    }


    private boolean isMainThread() {
        return mainThreadSupport == null || mainThreadSupport.isMainThread();
    }


    public void invokeSubscriber(PendingPost pendingPost) {
        Object event = pendingPost.event;
        Subscription subscription = pendingPost.subscription;
        PendingPost.releasePendingPost(pendingPost);
        if (subscription.active) {
            invokeSubscriber(subscription, event);
        }
    }

    private void invokeSubscriber(Subscription subscription, Object event) {
        try {
            subscription.subscriberMethod.method.invoke(subscription.subscriber, event);
        } catch (InvocationTargetException e) {
            handlerSubscriberException(subscription, event, e.getCause());
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("unexpected exception", e);
        }
    }

    private void handlerSubscriberException(Subscription subscription, Object event, Throwable cause) {
        if (event instanceof SubscriberExceptionEvent) {
            if (logSubscriberExceptions) {

            }
        } else {
            if (throwsSubscriberException) {

            }
            if (logSubscriberExceptions) {

            }
            if (sendSubscriberExceptionEvent) {
                SubscriberExceptionEvent exceptionEvent = new SubscriberExceptionEvent(this, cause, event, subscription.subscriber);
                post(exceptionEvent);
            }
        }
    }

    private void post(Object event) {
        PostingThreadState postingThreadState = currentPostingThreadState.get();
        List<Object> eventQueue = postingThreadState.eventQueue;
        eventQueue.add(event);
        if (!postingThreadState.isPosting) {
            postingThreadState.isMainThread = isMainThread();
            postingThreadState.isPosting = true;
            if (postingThreadState.canceled) {
                throw new EventBusException("intern error,Abort state was not reset");
            }
            try {
                while (!eventQueue.isEmpty()) {
                    postSingleEvent(eventQueue.remove(0), postingThreadState);
                }
            } finally {
                postingThreadState.isMainThread = false;
                postingThreadState.isPosting = false;
            }
        }
    }

    private void postSingleEvent(Object event, PostingThreadState postingThreadState) throws Error {
        Class<?> eventClass = event.getClass();
        boolean subscriptionFound = false;
        if (eventInheritance) {
            List<Class<?>> eventTypes = lookupAllEventTypes(eventClass);
            int countTypes = eventTypes.size();
            for (int i = 0; i < countTypes; i++) {
                Class<?> clazz = eventTypes.get(i);
                subscriptionFound |= postingSingleEventForEventType(event, postingThreadState, clazz);
            }
        } else {
            subscriptionFound = postingSingleEventForEventType(event, postingThreadState, eventClass);
        }
        if (!subscriptionFound) {
            if (sendNoSubscriberEvent && eventClass != NoSubscriberEvent.class && eventClass != SubscriberExceptionEvent.class) {
                post(new NoSubscriberEvent(this, event));
            }
        }
    }

    private boolean postingSingleEventForEventType(Object event, PostingThreadState postingThreadState, Class<?> eventClass) {
        CopyOnWriteArrayList<Subscription> subscriptions;
        synchronized (this){
            subscriptions=subscriptionByEventType.get(eventClass);
        }
        if(subscriptions!=null && !subscriptions.isEmpty()){
            for (Subscription subscription:subscriptions) {
                postingThreadState.event=event;
                postingThreadState.subscription=subscription;
                boolean aborted;
                try {
                    postToSubscription(subscription,event,isMainThread());
                    aborted=postingThreadState.canceled;
                }finally {
                    postingThreadState.event=null;
                    postingThreadState.subscription=null;
                    postingThreadState.canceled=false;
                }
                if(aborted){
                    break;
                }
            }
            return true;
        }
        return false;
    }

    private static List<Class<?>> lookupAllEventTypes(Class<?> eventClass) {
        synchronized (eventTypeCache){
            List<Class<?>> eventTypes=eventTypeCache.get(eventClass);
            if(eventTypes==null){
                eventTypes=new ArrayList<>();
                Class<?> clazz=eventClass;
                while (clazz!=null){
                    eventTypes.add(clazz);
                    addInterface(eventTypes,clazz.getInterfaces());
                    clazz=clazz.getSuperclass();
                }
            }
            return eventTypes;
        }
    }

    private static void addInterface(List<Class<?>> eventTypes, Class<?>[] interfaces) {
        for (Class<?> interfaceClass :interfaces) {
            if(eventTypes.contains(interfaceClass)){
                eventTypes.add(interfaceClass);
                addInterface(eventTypes,interfaceClass.getInterfaces());
            }
        }
    }

    public void removeAllStickyEvent(){
        synchronized (stickyEvents){
            stickyEvents.clear();
        }
    }



    public ExecutorService getExecutorService() {
        return executorService;
    }

    public Logger getLogger() {
        return logger;
    }

    @Override
    public String toString() {
        return "MyEventBus{" +
                "eventInheritance=" + eventInheritance +
                ", indexCount=" + indexCount +
                '}';
    }

    final static class PostingThreadState {
        final List<Object> eventQueue = new ArrayList<>();
        boolean isPosting;
        boolean isMainThread;
        Subscription subscription;
        Object event;
        boolean canceled;
    }
}
