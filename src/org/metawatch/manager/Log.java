package org.metawatch.manager;

public class Log {
    private static boolean enabled = false;

    public static void d(String tag, String msg) {
	if (enabled) {
	    android.util.Log.d(tag, msg);
	}
    }

    public static void w(String tag, String msg) {
	if (enabled) {
	    android.util.Log.w(tag, msg);
	}
    }

    public static void w(String tag, String msg, Throwable t) {
	if (enabled) {
	    android.util.Log.w(tag, msg, t);
	}
    }

    public static void v(String tag, String msg) {
	if (enabled) {
	    android.util.Log.v(tag, msg);
	}
    }

    public static void i(String tag, String msg) {
	if (enabled) {
	    android.util.Log.i(tag, msg);
	}
    }

    public static void e(String tag, String msg) {
	if (enabled) {
	    android.util.Log.e(tag, msg);
	}
    }

    public static void e(String tag, String msg, Throwable t) {
	if (enabled) {
	    android.util.Log.e(tag, msg, t);
	}
    }
}
