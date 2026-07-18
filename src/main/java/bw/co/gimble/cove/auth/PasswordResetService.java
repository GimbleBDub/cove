package bw.co.gimble.cove.auth;

import bw.co.gimble.cove.auth.dto.ForgotPasswordRequest;
import bw.co.gimble.cove.auth.dto.ResetPasswordRequest;
import bw.co.gimble.cove.common.ApiException;
import bw.co.gimble.cove.common.ErrorCode;
import bw.co.gimble.cove.mail.EmailService;
import bw.co.gimble.cove.security.TokenHasher;
import bw.co.gimble.cove.user.User;
import bw.co.gimble.cove.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final String RESET_PREFIX = "auth:reset:";

    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;
    private final RefreshTokenService refreshTokenService;
    private final TokenHasher tokenHasher;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.jwt.reset-token-ttl-minutes:30}")
    private long resetTokenTtlMinutes;

    @Value("${app.password-reset.frontend-url}")
    private String resetFrontendUrl;


    public void requestReset(ForgotPasswordRequest request) {
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(request.email());


        if (userOpt.isEmpty()) {
            log.debug("Password reset requested for an email with no matching account");
            return;
        }

        User user = userOpt.get();
        String rawToken = tokenHasher.generateRawToken();
        String hash = tokenHasher.hash(rawToken);

        redisTemplate.opsForValue().set(
                RESET_PREFIX + hash,
                user.getId().toString(),
                Duration.ofMinutes(resetTokenTtlMinutes)
        );

        String resetLink = resetFrontendUrl + "?token=" + rawToken;
        emailService.sendPasswordResetEmail(user.getEmail(), resetLink);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String hash = tokenHasher.hash(request.token());
        String key = RESET_PREFIX + hash;
        String userId = redisTemplate.opsForValue().get(key);

        if (userId == null) {
            throw ApiException.badRequest(ErrorCode.AUTH_RESET_TOKEN_INVALID, "This reset link is invalid or has expired");
        }

        redisTemplate.delete(key);

        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> ApiException.badRequest(ErrorCode.AUTH_RESET_TOKEN_INVALID, "This reset link is invalid or has expired"));

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);

        refreshTokenService.revokeAllForUser(user.getId());
    }
}