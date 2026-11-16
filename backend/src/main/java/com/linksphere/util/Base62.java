package com.linksphere.util;

import java.security.SecureRandom;
import java.util.Random;

public final class Base62 {

    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BASE = ALPHABET.length();
    private static final Random RANDOM = new SecureRandom();

    private Base62() {}

    /**
     * Encodes a positive long value to Base62 string.
     */
    public static String fromBase10(long number) {
        if (number == 0) {
            return String.valueOf(ALPHABET.charAt(0));
        }

        StringBuilder sb = new StringBuilder();
        long n = Math.abs(number);
        while (n > 0) {
            int remainder = (int) (n % BASE);
            sb.append(ALPHABET.charAt(remainder));
            n /= BASE;
        }
        return sb.reverse().toString();
    }

    /**
     * Generates a cryptographically random Base62 string of the specified length.
     */
    public static String generateRandom(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(BASE)));
        }
        return sb.toString();
    }
}
