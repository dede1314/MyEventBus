package com.example.myeventbus;

import androidx.annotation.Nullable;

import java.lang.reflect.Method;

/**
 * @author zhoujishi
 * @description
 * @date 2020/5/16
 */
public class SubscriberMethod {
    final Method method;
    final ThreadMode threadMode;
    final Class<?> eventType;
    final int priority;
    final boolean sticky;
    public String methodString;

    public SubscriberMethod(Method method, ThreadMode threadMode, Class<?> eventType, int priority, boolean sticky) {
        this.method = method;
        this.threadMode = threadMode;
        this.eventType = eventType;
        this.priority = priority;
        this.sticky = sticky;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (other == this) {
            return true;
        } else if (other instanceof SubscriberMethod) {
            checkMethodString();
            SubscriberMethod subscriberMethod= (SubscriberMethod) other;
            subscriberMethod.checkMethodString();
            return methodString.equals(subscriberMethod.methodString);
        } else {
            return false;
        }
    }

    private synchronized void checkMethodString() {
        if (methodString == null) {
            StringBuilder stringBuilder = new StringBuilder(64);
            stringBuilder.append(method.getDeclaringClass().getName());
            stringBuilder.append("#").append(method.getName());
            stringBuilder.append("(").append(eventType.getName());
            methodString = stringBuilder.toString();
        }
    }

    @Override
    public int hashCode() {
        return method.hashCode();
    }
}
