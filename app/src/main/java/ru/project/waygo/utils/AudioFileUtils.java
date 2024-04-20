package ru.project.waygo.utils;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class AudioFileUtils {
    public static FileDescriptor bytesToAudio(Context context, String fileName, String bytes) {
        if(bytes == null || bytes.isEmpty()){
            Log.d("CACHE", "cacheFiles: empty bytes");
            return null;
        }

        try {
            File tempMp3 = File.createTempFile(fileName, "mp3", context.getCacheDir());
            tempMp3.deleteOnExit();

            FileOutputStream outputStream = new FileOutputStream(tempMp3);
            outputStream.write(bytes.getBytes());
            outputStream.close();
            Log.i("AUDIO_GET_FILE", "bytesToAudio: cache get with file name " + fileName);

            return new FileInputStream(tempMp3).getFD();
        } catch (Exception e) {
            Log.e("AUDIO_GET_FILE", "bytesToAudio: fail get " + e.getLocalizedMessage());
        }

        return null;
    }

    public static String getAudioName(String objectType, long id) {
        return objectType + "_audio_" + id;
    }
}
