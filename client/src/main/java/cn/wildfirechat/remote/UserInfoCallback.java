/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.remote;

import android.os.Handler;
import android.os.Looper;

public abstract class UserInfoCallback<T> {

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public void onSuccess(final T t) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                onUiSuccess(t);
            }
        });
    }

    public void onFailure(final int code, final String message) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                onUiFailure(code, message);
            }
        });
    }

    public abstract void onUiSuccess(T t);

    public abstract void onUiFailure(int code, String msg);
}
