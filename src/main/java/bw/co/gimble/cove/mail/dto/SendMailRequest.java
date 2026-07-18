package bw.co.gimble.cove.mail.dto;

public record SendMailRequest(
        String to,
        String cc,
        String bcc,
        String subject,
        String body
) {}
