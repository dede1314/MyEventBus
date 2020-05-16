package com.example.myeventbus;

/**
 * @author zhoujishi
 * @description
 * @date 2020/5/16
 */
public class NoSubscriberEvent {
    public final MyEventBus myEventBus;
    public final Object originEvents;

    public NoSubscriberEvent(MyEventBus myEventBus, Object originEvents) {
        this.myEventBus = myEventBus;
        this.originEvents = originEvents;
    }
}
