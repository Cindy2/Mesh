package com.nxp.utils.io;

import android.widget.Toast;

public abstract class ToastHelper {
    static android.content.Context context;

    public static void setContext(android.content.Context context) {
        context = context;
    }

    public static void show(int resId) {
        Toast.makeText(context, resId, 0).show();
    }

    public static void show(String message) {
        show(message, 0);
    }

    public static void show(String message, int duration) {
        Toast.makeText(context, message, duration).show();
    }

    public static void show(int resId, int gravity) {
        Toast toast = Toast.makeText(context, resId, 0);
        toast.setGravity(gravity, 0, 0);
        toast.show();
    }
}
