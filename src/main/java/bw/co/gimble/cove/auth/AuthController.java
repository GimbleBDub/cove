package bw.co.gimble.cove.auth;

import bw.co.gimble.cove.auth.dto.*;
import bw.co.gimble.cove.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(authService.signup(request)));
    }

    /** Step 1 of login: verifies credentials and issues tokens once email is verified. */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(request)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.refresh(request)));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }

    /** Signed-in password change; also clears the "must change temporary password" flag. */
    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody bw.co.gimble.cove.auth.dto.ChangePasswordRequest request) {
        authService.changePassword(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestReset(request);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<SimpleMessageResponse>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.verifyEmail(request)));
    }

    @GetMapping("/verify-email-link")
    public org.springframework.web.servlet.view.RedirectView verifyEmailLinkGet(@RequestParam String token) {
        return new org.springframework.web.servlet.view.RedirectView(authService.webVerifyRedirect(token));
    }

    @PostMapping("/verify-email-link")
    public ResponseEntity<ApiResponse<SimpleMessageResponse>> verifyEmailLink(@Valid @RequestBody EmailLinkRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.verifyEmailLink(request)));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<SimpleMessageResponse>> resendVerification(@Valid @RequestBody ResendOtpRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.resendVerification(request)));
    }
}