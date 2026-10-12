package bw.co.gimble.cove.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record EmailLinkRequest(@NotBlank String token) {}
