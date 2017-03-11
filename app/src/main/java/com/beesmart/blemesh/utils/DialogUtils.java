package com.beesmart.blemesh.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import com.beesmart.blemesh.R;

/**
 * Created by alphawong on 2016/4/11.
 */
public class DialogUtils {
    private static AlertDialog.Builder createEditTextDialog(Context context, String title, String message){
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_addgroup,null);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title).setMessage(message);
        builder.setView(view);
        return builder;
    }
}
