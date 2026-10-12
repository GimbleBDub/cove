package bw.co.gimble.cove.applications.dto;

import jakarta.validation.constraints.NotBlank;

public record SignTextRequest(@NotBlank String typedName) {}
