package com.example.myeventbus;

import java.util.logging.Level;

/**
 * @author zhoujishi
 * @description
 * @date 2020/5/15
 */
public class AndroidLogger implements Logger {
    public static final boolean ANDROID_LOG_AVAILABLE;

    static {
        boolean android = false;
        try {
            android = Class.forName("android.util.log") != null;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        ANDROID_LOG_AVAILABLE = android;
    }

    public static boolean isAndroidLogAvailable() {
        return ANDROID_LOG_AVAILABLE;
    }

    private final String tag;

    public AndroidLogger(String tag) {
        this.tag = tag;
    }


    @Override
    public void log(Level level, String msg) {
        if (level != Level.OFF) {
            // TODO
        }
    }

    @Override
    public void log(Level level, String msg, Throwable throwable) {
// TODO
    }
}
