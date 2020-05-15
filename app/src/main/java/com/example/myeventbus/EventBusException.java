package com.example.myeventbus;

/**
 * @author zhoujishi
 * @description
 * @date 2020/5/15
 */
public class EventBusException extends RuntimeException {
    private static final long serialVersionUID = -2912559384646531479L;

    public EventBusException(String message) {
        super(message);
    }

    public EventBusException(Throwable cause) {
        super(cause);
    }

    public EventBusException(String message, Throwable cause) {
        super(message, cause);
    }
}
