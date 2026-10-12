package bw.co.gimble.cove.properties.dto;

import jakarta.validation.constraints.NotBlank;

public record UpsertPropertyTypeRequest(
        @NotBlank String code,
        @NotBlank String label,
        Boolean active
) {}
