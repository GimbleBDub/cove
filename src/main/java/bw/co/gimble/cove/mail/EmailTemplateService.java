package bw.co.gimble.cove.mail;

import bw.co.gimble.cove.mail.dto.SendMailRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.Year;

@Service
@RequiredArgsConstructor
public class EmailTemplateService {

    @Value("${app.base-url}")
    private String baseUrl;

    private final TemplateEngine templateEngine;

    public SendMailRequest buildPasswordResetMail(String recipientEmail, String rawToken) {
        Context ctx = new Context();
        ctx.setVariable("resetUrl", baseUrl + "/reset-password?token=" + rawToken);
        ctx.setVariable("year", Year.now().getValue());

        String body = templateEngine.process("email/password-reset", ctx);

        return new SendMailRequest(
                recipientEmail, null, null,
                "Reset your password", body
        );
    }

    public SendMailRequest buildPasswordChangedMail(String recipientEmail, String firstName) {
        Context ctx = new Context();
        ctx.setVariable("firstName", firstName);
        ctx.setVariable("year", Year.now().getValue());

        String body = templateEngine.process("email/password-changed", ctx);

        return new SendMailRequest(
                recipientEmail, null, null,
                "Your password was changed", body
        );
    }

    public SendMailRequest buildOtpMail(String recipientEmail, String otpCode) {
        Context ctx = new Context();
        ctx.setVariable("otpCode", otpCode);
        ctx.setVariable("year", Year.now().getValue());

        String body = templateEngine.process("email/otp", ctx);

        return new SendMailRequest(
                recipientEmail, null, null,
                "Your one-time login code", body
        );
    }
}