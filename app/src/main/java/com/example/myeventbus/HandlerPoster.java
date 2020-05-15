package com.example.myeventbus;


import android.os.Handler;
import android.os.Looper;

/**
 * @author zhoujishi
 * @description
 * @date 2020/5/15
 */
public class HandlerPoster extends Handler implements Poster {
    private final PendingPostQueue queue;
    private final int maxMilliisInsideHandlerMessage;
    private final MyEventBus myEventBus;
    private boolean handlerActive;

    protected HandlerPoster(MyEventBus myEventBus, Looper looper, int maxMilliisInsideHandlerMessage) {
        super(looper);
        this.myEventBus = myEventBus;
        this.maxMilliisInsideHandlerMessage = maxMilliisInsideHandlerMessage;
        queue = new PendingPostQueue();
    }


    @Override
    public void enqueue(Subscription subscription, Object event) {

    }
}
