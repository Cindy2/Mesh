package com.nxp.utils.io;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.concurrent.CancellationException;

import com.nxp.utils.exception.NotAvailableStorageException;
import com.nxp.utils.log.CSLog;


@SuppressLint({"SdCardPath"})
public class FileUtils {
    private static final int COPY_BUFFER_SIZE = 16384;

    public FileUtils() {
    }

    public static final File createFileIntelligently(Context context, String dirPath, String basename, String suffix) throws NotAvailableStorageException {
        if (basename == null) {
            basename = String.valueOf(System.currentTimeMillis());
        }

        File directory = getStorageDirIntelligently(context, dirPath);
        if (suffix == null) {
            suffix = "";
        }

        return new File(directory, basename + suffix);
    }

    public static final File getStorageDirIntelligently(Context context, String relativeDirPath) throws NotAvailableStorageException {
        File targetDir = null;
        if ("mounted".equals(Environment.getExternalStorageState())) {
            if (Environment.isExternalStorageEmulated()) {
                CSLog.v(FileUtils.class, "isExternalStorageEmulated == " + Environment.isExternalStorageEmulated());
            }

            targetDir = new File(Environment.getExternalStorageDirectory(), relativeDirPath);
        } else {
            targetDir = new File(getInternalFilesDir(context), relativeDirPath);
        }

        Log.v("targetDir", "targetDir == " + targetDir);
        if (!targetDir.exists()) {
            makeDirectory(targetDir, true);
        }

        return targetDir;
    }

    public static File getInternalFilesDir(Context context) {
        File file = context.getFilesDir();
        return file != null ? file : new File("/data/data/" + context.getPackageName() + "/" + FileUtils.DirType.FILES.dir);
    }

    public static final void makeDirectory(File directory, boolean force) throws NotAvailableStorageException {
        if (!directory.exists() && !directory.mkdir()) {
            throw new NotAvailableStorageException("failed mkdir.(path = " + directory.getAbsolutePath() + ")");
        } else {
            if (!directory.isDirectory()) {
                if (!force) {
                    throw new NotAvailableStorageException("failed mkdir. exists file.(path = " + directory.getAbsolutePath() + ")");
                }

                if (!directory.delete() && !directory.mkdir()) {
                    throw new NotAvailableStorageException("failed mkdir. exists file.(path = " + directory.getAbsolutePath() + ")");
                }
            }

        }
    }

    public static final long getAvailableSize() {
        StatFs statFs = new StatFs(Environment.getExternalStorageDirectory().getAbsolutePath());
        return (long) statFs.getBlockSize() * (long) statFs.getAvailableBlocks();
    }

    public static final void deleteRecursive(File fileOrDir) {
        if (fileOrDir != null) {
            if (fileOrDir.isDirectory()) {
                File[] var4;
                int var3 = (var4 = fileOrDir.listFiles()).length;

                for (int var2 = 0; var2 < var3; ++var2) {
                    File child = var4[var2];
                    deleteRecursive(child);
                }
            }

            fileOrDir.delete();
        }
    }

    public static void copyFile(File sourceFile, File destFile) throws IOException {
        if (!destFile.exists()) {
            destFile.createNewFile();
        }

        FileChannel source = null;
        FileChannel destination = null;

        try {
            source = (new FileInputStream(sourceFile)).getChannel();
            destination = (new FileOutputStream(destFile)).getChannel();
            destination.transferFrom(source, 0L, source.size());
        } finally {
            if (source != null) {
                source.close();
            }

            if (destination != null) {
                destination.close();
            }

        }

    }

    public static void copy(InputStream from, OutputStream to, FileUtils.ICancellable cancellable) throws IOException {
        byte[] buffer = new byte[16384];

        int read;
        while ((read = from.read(buffer)) != -1) {
            if (read > 0) {
                to.write(buffer, 0, read);
                if (cancellable != null && cancellable.isCancelled()) {
                    throw new CancellationException();
                }
            }
        }

    }

    public static enum DirType {
        CACHE("cache"),
        FILES("files");

        String dir;

        private DirType(String dir) {
            this.dir = dir;
        }
    }

    public interface ICancellable {
        boolean isCancelled();
    }
}
