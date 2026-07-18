package bw.co.gimble.cove.mail;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("!local")
@RequiredArgsConstructor
public class EmailService  {

    private final EmailTemplateService emailTemplateService;
    private final MailDispatchService mailDispatchService;

    public void sendPasswordResetEmail(String toEmail, String rawToken) {
        mailDispatchService.send(emailTemplateService.buildPasswordResetMail(toEmail, rawToken));
    }

    public void sendPasswordChangedNotification(String toEmail, String firstName) {
        mailDispatchService.send(emailTemplateService.buildPasswordChangedMail(toEmail, firstName));
    }

    public void sendOtpEmail(String toEmail, String otpCode) {
        mailDispatchService.send(emailTemplateService.buildOtpMail(toEmail, otpCode));
    }
}

