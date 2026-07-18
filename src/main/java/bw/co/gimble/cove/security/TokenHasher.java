package bw.co.gimble.cove.security;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Generates opaque bearer tokens (refresh tokens, password reset tokens) and hashes
 * them for storage. We never persist the raw token - only its SHA-256 hash - so a
 * database leak alone doesn't hand out usable tokens. The raw value is returned to
 * the client exactly once, at issuance.
 */
@Component
public class TokenHasher {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public String generateRawToken() {
        byte[] bytes = new byte[64];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}