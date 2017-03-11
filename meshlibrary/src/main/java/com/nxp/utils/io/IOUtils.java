package com.nxp.utils.io;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLConnection;
import java.nio.channels.Selector;


public class IOUtils {
    public static final int EOF = -1;
    public static final char DIR_SEPARATOR_UNIX = '/';
    public static final char DIR_SEPARATOR_WINDOWS = '\\';
    public static final char DIR_SEPARATOR = File.separatorChar;


    public static final String LINE_SEPARATOR_UNIX = "\n";


    public static final String LINE_SEPARATOR_WINDOWS = "\r\n";


    public static void close(URLConnection conn) {
        if ((conn instanceof HttpURLConnection)) {
            ((HttpURLConnection) conn).disconnect();
        }
    }


    public static void closeQuietly(Reader input) {
        closeQuietly(input);
    }


    public static void closeQuietly(Writer output) {
        closeQuietly(output);
    }


    public static void closeQuietly(InputStream input) {
        closeQuietly(input);
    }


    public static void closeQuietly(OutputStream output) {
        closeQuietly(output);
    }


    public static void closeQuietly(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException localIOException) {
        }
    }


    public static void closeQuietly(Socket sock) {
        if (sock != null) {
            try {
                sock.close();
            } catch (IOException localIOException) {
            }
        }
    }


    public static void closeQuietly(Selector selector) {
        if (selector != null) {
            try {
                selector.close();
            } catch (IOException localIOException) {
            }
        }
    }


    public static void closeQuietly(ServerSocket sock) {
        if (sock != null) {
            try {
                sock.close();
            } catch (IOException localIOException) {
            }
        }
    }
}
