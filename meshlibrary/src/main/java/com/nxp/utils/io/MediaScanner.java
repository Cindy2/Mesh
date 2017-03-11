package com.nxp.utils.io;

import android.content.Context;
import android.media.MediaScannerConnection;

import java.io.File;


public class MediaScanner {
    public static void scanFile(Context context, File file) {
        MediaScannerConnection.scanFile(context, new String[]{file.getAbsolutePath()}, null, null);
    }
}
