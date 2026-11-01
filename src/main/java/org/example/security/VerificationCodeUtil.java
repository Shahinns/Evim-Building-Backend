package org.example.security;

import java.security.SecureRandom;

public final class VerificationCodeUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    private VerificationCodeUtil() {}

    public static String generateSixDigitCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }
}
