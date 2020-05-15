package com.example.myeventbus;

import android.os.Looper;

import java.util.logging.Level;

/**
 * @author zhoujishi
 * @description
 * @date 2020/5/15
 */
public interface Logger {

    void log(Level level, String msg);

    void log(Level level, String msg, Throwable throwable);


    class JavaLogger implements Logger {
        protected final java.util.logging.Logger logger;

        public JavaLogger(String tag) {
            this.logger = java.util.logging.Logger.getLogger(tag);
        }

        @Override
        public void log(Level level, String msg) {
            logger.log(level,msg);
        }

        @Override
        public void log(Level level, String msg, Throwable throwable) {
            logger.log(level, msg, throwable);
        }
    }

    class SystemLogger implements Logger{
        @Override
        public void log(Level level, String msg) {
            System.out.println("["+level+"] "+msg);
        }

        @Override
        public void log(Level level, String msg, Throwable throwable) {
            System.out.println("["+level+"] "+msg);
            throwable.printStackTrace(System.out);
        }
    }

    class Default{
        public static Logger get(){
            return AndroidLogger.isAndroidLogAvailable() && getAndroidMainLooperOrNull()!=null
                    ? new AndroidLogger("EventBus"):new Logger.SystemLogger();
        }

        static Object getAndroidMainLooperOrNull(){
            try {
                return Looper.getMainLooper();
            }catch (RuntimeException e){
                return null;
            }
        }
    }
}
