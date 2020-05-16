package com.example.myeventbus;

/**
 * @author zhoujishi
 * @description
 * @date 2020/5/16
 */
public class SubscriberExceptionEvent {

    private final MyEventBus myEventBus;
    public final Throwable throwable;
    public final Object causingEvent;
    public final Object causingSubscriber;

    public SubscriberExceptionEvent(MyEventBus myEventBus, Throwable throwable, Object causingEvent, Object causingSubscriber) {
        this.myEventBus = myEventBus;
        this.throwable = throwable;
        this.causingEvent = causingEvent;
        this.causingSubscriber = causingSubscriber;
    }
}
