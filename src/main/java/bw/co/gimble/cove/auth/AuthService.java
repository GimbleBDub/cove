package bw.co.gimble.cove.auth;

import bw.co.gimble.cove.auth.dto.*;
import bw.co.gimble.cove.common.ApiException;
import bw.co.gimble.cove.common.ErrorCode;
import bw.co.gimble.cove.events.UserRegisteredEvent;
import bw.co.gimble.cove.mail.EmailService;
import bw.co.gimble.cove.security.JwtService;
import bw.co.gimble.cove.user.User;
import bw.co.gimble.cove.user.UserRepository;
import bw.co.gimble.cove.user.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final OtpService otpService;
    private final EmailService emailService;
    private final ApplicationEventPublisher eventPublisher;

    // ─── SIGNUP ─────────────────────────────────────────────────

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw ApiException.conflict(ErrorCode.EMAIL_ALREADY_REGISTERED, "An account with this email already exists");
        }

        User user = User.builder()
                .email(request.email().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.password()))
                .phone(request.phone())
                .build();
        user = userRepository.save(user);

        log.info("New account registered: {}", user.getEmail());

        // Deliberately fired here, not in a listener downstream of login - signup is the
        // one moment we know for certain this is a brand-new account.
        eventPublisher.publishEvent(new UserRegisteredEvent(user.getId(), user.getEmail()));

        return buildAuthResponse(user, refreshTokenService.issueNew(user));
    }

    // ─── LOGIN (password check → email OTP → tokens) ───────────

    /** Step 1: verifies credentials, sends a one-time code to the account's email. */
    public LoginAttemptResponse initiateLogin(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> ApiException.unauthorized(ErrorCode.AUTH_INVALID_CREDENTIALS, "Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw ApiException.unauthorized(ErrorCode.AUTH_INVALID_CREDENTIALS, "Invalid email or password");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw ApiException.forbidden(ErrorCode.AUTH_ACCOUNT_DISABLED, "This account is not active");
        }

        String otp = otpService.generateAndStore(user.getEmail());
        emailService.sendOtpEmail(user.getEmail(), otp);

        return new LoginAttemptResponse(true);
    }

    /** Step 2: verifies the OTP, issues access + refresh tokens. */
    @Transactional
    public AuthResponse completeLogin(OtpVerifyRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> ApiException.unauthorized(ErrorCode.AUTH_INVALID_CREDENTIALS, "Invalid credentials"));

        // Re-checked here even though initiateLogin already checked it - the account could
        // have been disabled in the window between the two steps.
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw ApiException.forbidden(ErrorCode.AUTH_ACCOUNT_DISABLED, "This account is not active");
        }

        if (!otpService.verify(user.getEmail(), request.otp())) {
            throw ApiException.unauthorized(ErrorCode.AUTH_INVALID_OTP, "Invalid or expired OTP");
        }

        return buildAuthResponse(user, refreshTokenService.issueNew(user));
    }

    public SimpleMessageResponse resendOtp(ResendOtpRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> ApiException.unauthorized(ErrorCode.AUTH_INVALID_CREDENTIALS, "Invalid credentials"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw ApiException.forbidden(ErrorCode.AUTH_ACCOUNT_DISABLED, "This account is not active");
        }

        String otp = otpService.generateAndStore(user.getEmail());
        emailService.sendOtpEmail(user.getEmail(), otp);

        return new SimpleMessageResponse("OTP resent.");
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