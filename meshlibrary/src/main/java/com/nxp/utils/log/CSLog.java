package com.nxp.utils.log;

import android.text.TextUtils;


public class CSLog {
    public static boolean isOpen = false;


    public static final void e(Class<?> object, String msg) {
        if (isOpen) {
            Logger.e(getTag(object, ""), msg);
        }
    }


    public static final void e(Class<?> object, String tag, String msg) {
        if (isOpen) {
            Logger.e(getTag(object, tag), msg);
        }
    }


    public static final void w(Class<?> object, String msg) {
        if (isOpen) {
            Logger.w(getTag(object, ""), msg);
        }
    }


    public static final void w(Class<?> object, String tag, String msg) {
        if (isOpen) {
            Logger.w(getTag(object, tag), msg);
        }
    }


    public static final void i(Class<?> object, String msg) {
        if (isOpen) {
            Logger.i(getTag(object, ""), msg);
        }
    }


    public static final void i(Class<?> object, String tag, String msg) {
        if (isOpen) {
            Logger.i(getTag(object, tag), msg);
        }
    }


    public static final void d(Class<?> object, String msg) {
        if (isOpen) {
            Logger.d(getTag(object, ""), msg);
        }
    }


    public static final void d(Class<?> object, String tag, String msg) {
        if (isOpen) {
            Logger.d(getTag(object, tag), msg);
        }
    }


    public static final void v(Class<?> object, String msg) {
        if (isOpen) {
            Logger.v(getTag(object, ""), msg);
        }
    }

    public static final void v(Class<?> object, String tag, String msg) {
        if (isOpen) {
            Logger.v(getTag(object, tag), msg);
        }
    }

    public static final void n(Class<?> object, String msg) {
        if (isOpen) {
            Logger.n(getTag(object, ""), msg);
        }
    }

    public static final void n(Class<?> object, String tag, String msg) {
        if (isOpen) {
            Logger.n(getTag(object, tag), msg);
        }
    }

    private static final String getTag(Class<?> object, String tag) {
        String suffix = "";
        if (!TextUtils.isEmpty(tag)) {
            suffix = "_" + tag;
        }
        return "__" + object.getSimpleName() + suffix;
    }
}
