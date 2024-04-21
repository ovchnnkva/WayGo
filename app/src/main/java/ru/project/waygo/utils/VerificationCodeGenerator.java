package ru.project.waygo.utils;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class VerificationCodeGenerator {
    private static final int MAX_VALUE = 999999;
    private static final int MIN_VALUE = 100000;

    public static String generate() {
        return ThreadLocalRandom.current().nextLong(MIN_VALUE, MAX_VALUE) + "";
    }
}
