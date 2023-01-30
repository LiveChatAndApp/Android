package cn.wildfire.chat.kit.utils;

import android.util.Log;

import cn.wildfire.chat.kit.BuildConfig;

public class LogHelper {
    private static boolean ISDEBUG = BuildConfig.DEBUG;

    public static void e(String tag, String msg) {
        if (!ISDEBUG) {
            return;
        }
        Log.e(tag, msg);
    }

    public static void d(String tag, String msg) {
        if (!ISDEBUG) {
            return;
        }
        Log.d(tag, msg);
    }

    public static void i(String tag, String msg) {
        if (!ISDEBUG) {
            return;
        }
        Log.i(tag, msg);
    }
}
