package ru.project.utils;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.util.stream.Collectors;

public class CacheUtils {
    public static void cacheFiles(Context context, String fileName, String bytes) {
        if(bytes == null || bytes.isEmpty()) return;

        File cacheDir = context.getCacheDir();
        File file = new File(cacheDir, fileName + ".txt");

        try {
            file.createNewFile();
            FileOutputStream outputStream = new FileOutputStream(file);
            outputStream.write(bytes.getBytes());
            outputStream.close();
            Log.i("CACHE_SAVE", "cacheFiles: cache save with file name " + fileName);
        } catch (Exception e) {
            Log.e("CACHE_SAVE", "cacheFiles: fail cache " + e.getLocalizedMessage());
        }
    }

    public static byte[] getFileCache(Context context, String fileName) {
        File cacheDir = context.getCacheDir();
        File file = new File(cacheDir, fileName + ".txt");

        try {
            FileInputStream inputStream = new FileInputStream(file);
            byte[] buffer = new byte[inputStream.available()];

            inputStream.read(buffer);
            inputStream.close();
            Log.i("CACHE_GET", "getFileCache: cache find with file name " + fileName);
            return buffer;
        } catch (Exception e) {
            Log.e("CACHE_GET", "cacheFiles: fail cache " + e.getLocalizedMessage());
        }

        return null;
    }

    public static FileDescriptor getFileDescriptor(Context context, String fileName) {
        File cacheDir = context.getCacheDir();
        File file = new File(cacheDir, fileName + ".txt");

        try {
            FileInputStream inputStream = new FileInputStream(file);
            byte[] buffer = new byte[inputStream.available()];

            inputStream.read(buffer);
            inputStream.close();
            Log.i("CACHE_GET", "getFileCache: cache find with file name " + fileName);
            return inputStream.getFD();
        } catch (Exception e) {
            Log.e("CACHE_GET", "cacheFiles: fail cache " + e.getLocalizedMessage());
        }

        return null;
    }

    public static String getFileName(String objectType, long id) {
        return objectType + "_" + id;
    }

    public static boolean isExistsCache(Context context, String fileName) {
        File cacheDir = context.getCacheDir();
        File file = new File(cacheDir, fileName);

        return file.exists() && file.isFile();
    }
}
