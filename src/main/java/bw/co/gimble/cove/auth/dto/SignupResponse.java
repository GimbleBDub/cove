package bw.co.gimble.cove.auth.dto;

import lombok.Builder;

import java.util.UUID;

@Builder
public record SignupResponse(
        UUID userId,
        String email,
        String message
) {}
