package bw.co.gimble.cove.properties.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreatePropertyRequest(
        @NotNull UUID typeId,
        String description,
        Integer bedrooms,
        Integer bathrooms,
        Integer parkingSpaces,
        BigDecimal sizeSqm,
        String country,
        UUID cityId,
        UUID areaId,
        String city,
        String suburb,
        String street,
        BigDecimal latitude,
        BigDecimal longitude,
        UUID managingOrganisationId,
        @Valid List<OwnerSplitRequest> owners
) {}
