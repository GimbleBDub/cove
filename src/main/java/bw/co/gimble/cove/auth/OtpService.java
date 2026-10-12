package bw.co.gimble.cove.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

/**
 * One-time login codes, stored in Redis - single-use, short TTL, no relational value,
 * same reasoning as refresh tokens and password reset tokens. Key: auth:otp:{templates}.
 */
@Service
@RequiredArgsConstructor
public class OtpService {

    private static final String OTP_PREFIX = "auth:otp:";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;

    @Value("${app.otp.ttl-minutes:5}")
    private long otpTtlMinutes;

    public String generateAndStore(String email) {
        return generateAndStore(email, Duration.ofMinutes(otpTtlMinutes));
    }

    public String generateAndStore(String keyPart, Duration ttl) {
        String otp = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        redisTemplate.opsForValue().set(key(keyPart), otp, ttl);
        return otp;
    }

    public boolean verify(String email, String submittedOtp) {
        String key = key(email);
        String stored = redisTemplate.opsForValue().get(key);
        if (stored == null || !stored.equals(submittedOtp)) {
            return false;
        }
        redisTemplate.delete(key);
        return true;
    }

    public void storeValue(String redisKey, String value, Duration ttl) {
        redisTemplate.opsForValue().set(OTP_PREFIX + redisKey.toLowerCase(), value, ttl);
    }

    public String consumeValue(String redisKey) {
        String key = OTP_PREFIX + redisKey.toLowerCase();
        String stored = redisTemplate.opsForValue().get(key);
        if (stored == null) {
            return null;
        }
        redisTemplate.delete(key);
        return stored;
    }

    private String key(String email) {
        return OTP_PREFIX + email.toLowerCase();
    }
}
