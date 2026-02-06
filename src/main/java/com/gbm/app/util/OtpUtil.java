package com.gbm.app.util;

import java.security.SecureRandom;

public final class OtpUtil {
    private static final SecureRandom RANDOM = new SecureRandom();

    private OtpUtil() {
    }

    public static String generateOtp() {
        int value = 1000 + RANDOM.nextInt(9000);
        return String.valueOf(value);
    }
}
