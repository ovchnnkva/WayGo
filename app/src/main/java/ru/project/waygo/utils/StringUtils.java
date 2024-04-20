package ru.project.waygo.utils;

import android.annotation.SuppressLint;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class StringUtils {

    @SuppressLint("DefaultLocale")
    public static String getAudioTimeString(int milliseconds) {
        return padWithZeros("" + TimeUnit.MILLISECONDS.toMinutes(milliseconds)) + ":" +
                padWithZeros("" + (TimeUnit.MILLISECONDS.toSeconds(milliseconds) - TimeUnit.MINUTES
                        .toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliseconds))));
    }

    private static String padWithZeros(String time) {
        if (time.length() < 2) {
            return "0" + time;
        } else return time;
    }
}
