package bw.co.gimble.cove.staff;

import java.security.SecureRandom;

/** Readable one-time passwords (no 0/O/1/l) for accounts created on someone's behalf. */
public final class TemporaryPasswords {
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private TemporaryPasswords() {}

    public static String generate() {
        StringBuilder sb = new StringBuilder("Cove-");
        for (int i = 0; i < 12; i++) {
            if (i == 4 || i == 8) sb.append('-');
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
