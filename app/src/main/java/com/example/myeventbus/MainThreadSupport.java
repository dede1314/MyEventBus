package com.example.myeventbus;

import android.os.Looper;

/**
 * @author zhoujishi
 * @description
 * @date 2020/5/15
 */
public interface MainThreadSupport {
    boolean isMainThread();

    Poster createPoster(MyEventBus eventBus);

    class AndroidHandlerMainThreadSupport implements MainThreadSupport{
        private final Looper looper;

        public AndroidHandlerMainThreadSupport(Looper looper) {
            this.looper = looper;
        }

        @Override
        public boolean isMainThread() {
            return looper==Looper.myLooper();
        }

        @Override
        public Poster createPoster(MyEventBus eventBus) {
            return new HandlerPoster(eventBus,looper,10);
        }
    }
}
