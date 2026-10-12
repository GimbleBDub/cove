package bw.co.gimble.cove.auth;

import java.util.UUID;
import bw.co.gimble.cove.auth.dto.*;
import bw.co.gimble.cove.common.ApiException;
import bw.co.gimble.cove.common.ErrorCode;
import bw.co.gimble.cove.events.UserRegisteredEvent;
import bw.co.gimble.cove.mail.EmailService;
import bw.co.gimble.cove.security.JwtService;
import bw.co.gimble.cove.security.TokenHasher;
import bw.co.gimble.cove.user.User;
import bw.co.gimble.cove.user.UserRepository;
import bw.co.gimble.cove.user.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class  AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final OtpService otpService;
    private final EmailService emailService;
    private final ApplicationEventPublisher eventPublisher;
    private final TokenHasher tokenHasher;

    @Value("${app.frontend-url:${app.password-reset.frontend-url:http://localhost:64045}}")
    private String frontendUrl;

    @Value("${app.public-api-url:http://localhost:8099}")
    private String publicApiUrl;

    private static final String EMAIL_LINK_PREFIX = "EMAIL_LINK:";

    // ─── SIGNUP ─────────────────────────────────────────────────

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw ApiException.conflict(ErrorCode.EMAIL_ALREADY_REGISTERED, "An account with this email already exists");
        }

        User user = User.builder()
                .email(request.email().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.password()))
                .phone(request.phone())
                .emailVerified(false)
                .phoneVerified(false)
                .build();
        user = userRepository.save(user);

        sendVerification(user);

        eventPublisher.publishEvent(new UserRegisteredEvent(user.getId(), user.getEmail()));

        return SignupResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .message("Check your email for a verification link. After you verify, you can log in.")
                .build();
    }

    @Transactional
    public SimpleMessageResponse verifyEmail(VerifyEmailRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> ApiException.unauthorized(ErrorCode.AUTH_INVALID_CREDENTIALS, "Invalid credentials"));

        if (!otpService.verify("EMAIL_VERIFY:" + user.getEmail(), request.code())) {
            throw ApiException.unauthorized(ErrorCode.AUTH_INVALID_OTP, "Invalid or expired verification code");
        }

        markEmailVerified(user);
        return new SimpleMessageResponse("Email verified. You can now log in.");
    }

    @Transactional
    public SimpleMessageResponse verifyEmailLink(EmailLinkRequest request) {
        markEmailVerified(consumeLinkToken(request.token()));
        return new SimpleMessageResponse("Email verified. You can now log in.");
    }

    public String webVerifyRedirect(String token) {
        try {
            markEmailVerified(consumeLinkToken(token));
            return frontendUrl + "/login?verified=1";
        } catch (RuntimeException ex) {
            return frontendUrl + "/login?error=verify";
        }
    }

    public SimpleMessageResponse resendVerification(ResendOtpRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> ApiException.unauthorized(ErrorCode.AUTH_INVALID_CREDENTIALS, "Invalid credentials"));

        if (user.isEmailVerified()) {
            throw ApiException.conflict(ErrorCode.ALREADY_VERIFIED, "This account is already verified");
        }

        sendVerification(user);
        return new SimpleMessageResponse("Verification link resent.");
    }

    // ─── LOGIN ────────────────────────────────────────────────────

    /** Password login after email verification. Issues tokens immediately. */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> ApiException.unauthorized(ErrorCode.AUTH_INVALID_CREDENTIALS, "Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw ApiException.unauthorized(ErrorCode.AUTH_INVALID_CREDENTIALS, "Invalid email or password");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw ApiException.forbidden(ErrorCode.AUTH_ACCOUNT_DISABLED, "This account is not active");
        }

        if (!user.isEmailVerified()) {
            throw ApiException.forbidden(ErrorCode.ACCOUNT_NOT_VERIFIED,
                    "Please verify your email from the link we sent you before logging in");
        }

        return buildAuthResponse(user, refreshTokenService.issueNew(user));
    }

    @Transactional
    public void changePassword(bw.co.gimble.cove.auth.dto.ChangePasswordRequest request) {
        UUID userId = bw.co.gimble.cove.security.AuthSupport.requireUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized(ErrorCode.AUTH_INVALID_CREDENTIALS, "Not signed in"));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw ApiException.badRequest(ErrorCode.AUTH_INVALID_CREDENTIALS, "Your current password is incorrect");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "Choose a password different from the current one");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setPasswordChangeRequired(false);
    }

    // ─── REFRESH / LOGOUT ───────────────────────────────────────

    /** Rotates the presented refresh token (single-use) and mints a fresh access token to match. */
    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        RefreshTokenService.IssuedToken issued = refreshTokenService.rotate(request.refreshToken());
        User user = userRepository.findById(issued.userId())
                .orElseThrow(() -> ApiException.unauthorized(ErrorCode.AUTH_INVALID_REFRESH_TOKEN, "Refresh token is invalid"));
        return buildAuthResponse(user, issued);
    }

    /** Revokes the session (refresh token family) the presented token belongs to. */
    @Transactional
    public void logout(RefreshRequest request) {
        refreshTokenService.revoke(request.refreshToken());
    }

    // ─── HELPERS ────────────────────────────────────────────────

    private void sendVerification(User user) {
        String rawToken = tokenHasher.generateRawToken();
        otpService.storeValue(EMAIL_LINK_PREFIX + rawToken, user.getEmail(), Duration.ofHours(24));
        String code = otpService.generateAndStore("EMAIL_VERIFY:" + user.getEmail(), Duration.ofHours(24));
        String link = publicApiUrl.replaceAll("/+$", "") + "/api/auth/verify-email-link?token=" + rawToken;
        emailService.sendVerificationEmail(user.getEmail(), link, code);
    }

    private User consumeLinkToken(String token) {
        String email = otpService.consumeValue(EMAIL_LINK_PREFIX + token);
        if (email == null) {
            throw ApiException.unauthorized(ErrorCode.AUTH_INVALID_OTP, "This verification link is invalid or has expired");
        }
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ApiException.unauthorized(ErrorCode.AUTH_INVALID_CREDENTIALS, "Invalid credentials"));
    }

    private void markEmailVerified(User user) {
        if (!user.isEmailVerified()) {
            user.setEmailVerified(true);
            userRepository.save(user);
            emailService.sendWelcomeEmail(user.getEmail());
        }
    }

    private AuthResponse buildAuthResponse(User user, RefreshTokenService.IssuedToken refreshToken) {
        String token = jwtService.issueAccessToken(user.getId(), user.getEmail(), user.getTokenVersion());
        return AuthResponse.builder()
                .accessToken(token)
                .expiresInSeconds(jwtService.getAccessTokenTtlSeconds())
                .refreshToken(refreshToken.rawToken())
                .refreshExpiresInSeconds(refreshTokenService.getRefreshTokenTtlSeconds())
                .userId(user.getId())
                .email(user.getEmail())
                .build();
    }
}