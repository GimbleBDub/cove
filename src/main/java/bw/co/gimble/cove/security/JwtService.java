package bw.co.gimble.cove.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Access tokens are short-lived and carry ONLY identity (userId, email, tokenVersion).
 * No roles are embedded - see AuthPrincipal for why. Refresh tokens are opaque,
 * random, stored hashed server-side (not implemented here - swap in a
 * RefreshTokenRepository when you add persistence for it).
 */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long accessTokenTtlSeconds;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-ttl-seconds:900}") long accessTokenTtlSeconds) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenTtlSeconds = accessTokenTtlSeconds;
    }

    public String issueAccessToken(UUID userId, String email, int tokenVersion) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("tv", tokenVersion)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTokenTtlSeconds)))
                .signWith(signingKey)
                .compact();
    }

    public Claims parseAndValidate(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getAccessTokenTtlSeconds() {
        return accessTokenTtlSeconds;
    }
}