package com.example.myeventbus;

/**
 * @author zhoujishi
 * @description
 * @date 2020/5/15
 */
interface Poster {
    void enqueue(Subscription subscription,Object event);
}
