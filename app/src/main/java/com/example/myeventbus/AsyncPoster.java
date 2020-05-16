package com.example.myeventbus;

/**
 * @author zhoujishi
 * @description
 * @date 2020/5/15
 */
public class AsyncPoster implements Runnable,Poster{
    private final PendingPostQueue queue;
    private final MyEventBus myEventBus;

    public AsyncPoster(MyEventBus myEventBus) {
        this.myEventBus=myEventBus;
        queue=new PendingPostQueue();
    }


    @Override
    public void enqueue(Subscription subscription, Object event) {
        PendingPost pendingPost=PendingPost.obtainPendingPost(subscription,event);
        queue.enqueue(pendingPost);
        myEventBus.getExecutorService().execute(this);
    }

    @Override
    public void run() {
        PendingPost pendingPost=queue.poll();
        if(pendingPost==null){
            throw new EventBusException("no pending post available");
        }
        myEventBus.invokeSubscriber(pendingPost);
    }
}
