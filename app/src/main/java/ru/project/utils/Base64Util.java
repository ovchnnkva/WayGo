package ru.project.utils;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.Base64;

public class Base64Util {
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static byte[] stringToByte(String encodeString) {
        return Base64.getDecoder().decode(encodeString);
    }
}
