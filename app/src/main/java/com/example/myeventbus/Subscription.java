package com.example.myeventbus;

import androidx.annotation.Nullable;

/**
 * @author zhoujishi
 * @description
 * @date 2020/5/15
 */
public class Subscription {
    final Object subscriber;
    final SubscriberMethod subscriberMethod;
    volatile boolean active;

    public Subscription(Object subscriber, SubscriberMethod subscriberMethod) {
        this.subscriber = subscriber;
        this.subscriberMethod = subscriberMethod;
        active=true;
    }

    @Override
    public boolean equals(Object other) {
        if(other instanceof Subscription){
            Subscription otherSubscription= (Subscription) other;
            return subscriber==otherSubscription.subscriber &&
                    subscriberMethod.equals(otherSubscription.subscriberMethod);
        }else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return subscriber.hashCode()+subscriberMethod.methodString.hashCode();
    }


}
