package bw.co.gimble.cove.auth.dto;

import lombok.Builder;

import java.util.UUID;

@Builder
public record AuthResponse(
        String accessToken,
        long expiresInSeconds,
        String refreshToken,
        long refreshExpiresInSeconds,
        UUID userId,
        String email
) {}