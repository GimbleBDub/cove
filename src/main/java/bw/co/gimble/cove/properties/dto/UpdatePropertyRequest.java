package bw.co.gimble.cove.properties.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdatePropertyRequest(
        UUID typeId,
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
        UUID managingOrganisationId
) {}
