package model.utils;

import java.util.Random;
import java.util.UUID;

public class IdGenerator {

    public static String randomId(String prefix, int suffixLength) {
        if (suffixLength <= 0) {
            suffixLength = 8;
        }
        String base = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        if (base.length() < suffixLength) {
            suffixLength = base.length();
        }
        return prefix + base.substring(0, suffixLength);
    }

    public static String randomDigits(String prefix, int digits) {
        if (digits <= 0) {
            digits = 4;
        }
        Random random = new Random();
        StringBuilder sb = new StringBuilder(prefix);
        for (int i = 0; i < digits; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
