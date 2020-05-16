package com.example.myeventbus;


import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;

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
        PendingPost pendingPost = PendingPost.obtainPendingPost(subscription, event);
        synchronized (this) {
            queue.enqueue(pendingPost);
            if (!handlerActive) {
                handlerActive = true;
                if (!sendMessage(obtainMessage())) {
                    throw new EventBusException("could not send handler message");
                }
            }
        }
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
        boolean rescheduled = false;
        try{
            long started=System.currentTimeMillis();
            while (true){
                PendingPost pendingPost=queue.poll();
                if(pendingPost==null){
                    synchronized (this){
                        pendingPost=queue.poll();
                        if(pendingPost==null){
                            handlerActive=false;
                            return;
                        }
                    }
                }
                myEventBus.invokeSubscriber(pendingPost);
                long timeInMethod=System.currentTimeMillis()-started;
                if(timeInMethod>maxMilliisInsideHandlerMessage){
                    if(!sendMessage(obtainMessage())){
                        throw new EventBusException("could not send handler message");
                    }
                    rescheduled=true;
                    return;
                }
            }
        }finally {
            handlerActive=rescheduled;
        }
    }
}
