package bw.co.gimble.cove.mail;

import bw.co.gimble.cove.mail.dto.SendMailRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;

import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailDispatchService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    public void send(SendMailRequest request) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(fromAddress);
            helper.setTo(request.to());
            if (request.cc() != null) {
                helper.setCc(request.cc());
            }
            if (request.bcc() != null) {
                helper.setBcc(request.bcc());
            }
            helper.setSubject(request.subject());
            helper.setText(request.body(), true);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send templates to {}: {}", request.to(), e.getMessage(), e);
        }
    }
}
