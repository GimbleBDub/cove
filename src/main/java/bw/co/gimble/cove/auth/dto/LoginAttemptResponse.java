package bw.co.gimble.cove.auth.dto;

public record LoginAttemptResponse(
        boolean otpSent
) {}