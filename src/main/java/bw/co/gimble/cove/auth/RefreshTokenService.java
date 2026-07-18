package bw.co.gimble.cove.auth;

import bw.co.gimble.cove.common.ApiException;
import bw.co.gimble.cove.common.ErrorCode;
import bw.co.gimble.cove.security.TokenHasher;
import bw.co.gimble.cove.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String TOKEN_PREFIX = "auth:refresh:";
    private static final String USED_PREFIX = "auth:refresh:used:";
    private static final String FAMILY_PREFIX = "auth:refresh:family:";
    private static final String USER_PREFIX = "auth:refresh:user:";

    private final StringRedisTemplate redisTemplate;
    private final TokenHasher tokenHasher;

    @Value("${app.jwt.refresh-ttl-days:30}")
    private long refreshTokenTtlDays;

    @Value("${app.jwt.refresh-reuse-window-seconds:120}")
    private long reuseDetectionWindowSeconds;

    public long getRefreshTokenTtlSeconds() {
        return refreshTokenTtlDays * 24 * 60 * 60;
    }

    private Duration refreshTtl() {
        return Duration.ofSeconds(getRefreshTokenTtlSeconds());
    }

    /** Issues the first refresh token of a new session (login/signup). Starts a new family. */
    public RefreshTokenService.IssuedToken issueNew(User user) {
        return issue(user.getId(), UUID.randomUUID());
    }

    /**
     * Rotates a refresh token: validates the presented raw token, deletes it (single-use),
     * and issues a replacement in the same family.
     */
    public RefreshTokenService.IssuedToken rotate(String rawToken) {
        String hash = tokenHasher.hash(rawToken);
        String tokenKey = TOKEN_PREFIX + hash;
        String record = redisTemplate.opsForValue().get(tokenKey);

        if (record == null) {
            String tombstone = redisTemplate.opsForValue().get(USED_PREFIX + hash);
            if (tombstone != null) {
                revokeFamily(UUID.fromString(tombstone.split(":", 2)[1]));
                throw ApiException.unauthorized(ErrorCode.AUTH_REFRESH_TOKEN_REUSED, "Refresh token has already been used; session revoked");
            }
            throw ApiException.unauthorized(ErrorCode.AUTH_INVALID_REFRESH_TOKEN, "Refresh token is invalid or has expired");
        }

        String[] parts = record.split(":", 2);
        UUID userId = UUID.fromString(parts[0]);
        UUID familyId = UUID.fromString(parts[1]);

        redisTemplate.delete(tokenKey);
        redisTemplate.opsForValue().set(USED_PREFIX + hash, record, Duration.ofSeconds(reuseDetectionWindowSeconds));

        return issue(userId, familyId);
    }

    /** Logout: revokes the whole session (family) the presented token belongs to. */
    public void revoke(String rawToken) {
        String hash = tokenHasher.hash(rawToken);
        String record = redisTemplate.opsForValue().get(TOKEN_PREFIX + hash);
        if (record == null) {
            return;
        }
        revokeFamily(UUID.fromString(record.split(":", 2)[1]));
    }

    /** Used by password reset: kill every session for the user, not just one family. */
    public void revokeAllForUser(UUID userId) {
        String userKey = USER_PREFIX + userId;
        Set<String> familyIds = redisTemplate.opsForSet().members(userKey);
        if (familyIds != null) {
            familyIds.forEach(fid -> revokeFamily(UUID.fromString(fid)));
        }
        redisTemplate.delete(userKey);
    }

    private void revokeFamily(UUID familyId) {
        String familyKey = FAMILY_PREFIX + familyId;
        String activeHash = redisTemplate.opsForValue().get(familyKey);
        if (activeHash != null) {
            redisTemplate.delete(TOKEN_PREFIX + activeHash);
        }
        redisTemplate.delete(familyKey);
    }

    private RefreshTokenService.IssuedToken issue(UUID userId, UUID familyId) {
        String rawToken = tokenHasher.generateRawToken();
        String hash = tokenHasher.hash(rawToken);
        Duration ttl = refreshTtl();

        redisTemplate.opsForValue().set(TOKEN_PREFIX + hash, userId + ":" + familyId, ttl);
        redisTemplate.opsForValue().set(FAMILY_PREFIX + familyId, hash, ttl);

        String userKey = USER_PREFIX + userId;
        redisTemplate.opsForSet().add(userKey, familyId.toString());
        redisTemplate.expire(userKey, ttl);

        return new RefreshTokenService.IssuedToken(userId, rawToken, Instant.now().plus(ttl));
    }

    public record IssuedToken(UUID userId, String rawToken, Instant expiresAt) {}
}
