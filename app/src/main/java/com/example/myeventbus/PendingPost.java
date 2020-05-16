package com.example.myeventbus;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zhoujishi
 * @description
 * @date 2020/5/16
 */
public class PendingPost {
    private final static List<PendingPost> pendingPostPool = new ArrayList<>();
    Object event;
    Subscription subscription;
    PendingPost next;


    public PendingPost(Object event, Subscription subscription) {
        this.event = event;
        this.subscription = subscription;
    }

    static PendingPost obtainPendingPost(Subscription subscription, Object event) {
        synchronized (pendingPostPool) {
            int size = pendingPostPool.size();
            if (size > 0) {
                PendingPost pendingPost = pendingPostPool.remove(size - 1);
                pendingPost.event = event;
                pendingPost.subscription = subscription;
                pendingPost.next = null;
                return pendingPost;
            }
            return new PendingPost(event, subscription);
        }
    }

    static void releasePendingPost(PendingPost pendingPost) {
        pendingPost.next = null;
        pendingPost.subscription = null;
        pendingPost.event = null;
        synchronized (pendingPostPool) {
            if (pendingPostPool.size() < 1000) {
                pendingPostPool.add(pendingPost);
            }
        }
    }

}
