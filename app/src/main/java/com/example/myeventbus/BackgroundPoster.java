package com.example.myeventbus;

import java.util.logging.Level;

/**
 * @author zhoujishi
 * @description
 * @date 2020/5/15
 */
public class BackgroundPoster implements Runnable,Poster{
    private final PendingPostQueue queue;
    private final MyEventBus myEventBus;
    private volatile boolean executorRunning;
    public BackgroundPoster(MyEventBus myEventBus) {
        this.myEventBus=myEventBus;
        queue=new PendingPostQueue();
    }

    @Override
    public void enqueue(Subscription subscription, Object event) {
        PendingPost pendingPost=PendingPost.obtainPendingPost(subscription,event);
        synchronized (this){
            queue.enqueue(pendingPost);
            if(!executorRunning){
                executorRunning=true;
                myEventBus.getExecutorService().execute(this);
            }
        }
    }

    @Override
    public void run() {
        try {
            try {
                while (true){
                    PendingPost pendingPost=queue.poll(1000);
                    if(pendingPost==null){
                        synchronized (this){
                            pendingPost=queue.poll();
                            if(pendingPost==null){
                                executorRunning=false;
                                return;
                            }
                        }
                    }
                    myEventBus.invokeSubscriber(pendingPost);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                myEventBus.getLogger().log(Level.WARNING,Thread.currentThread().getName()+"was interrupted ",e);
            }
        }finally {
            executorRunning=false;
        }
    }
}
