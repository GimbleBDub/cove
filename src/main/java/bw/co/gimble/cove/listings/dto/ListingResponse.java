package bw.co.gimble.cove.listings.dto;

import bw.co.gimble.cove.listings.ListingPurpose;
import bw.co.gimble.cove.listings.ListingStatus;
import bw.co.gimble.cove.listings.RentPeriod;
import bw.co.gimble.cove.properties.PropertyStatus;
import bw.co.gimble.cove.properties.dto.PropertyResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ListingResponse(
        UUID id,
        UUID propertyId,
        ListingPurpose purpose,
        ListingStatus status,
        boolean published,
        BigDecimal price,
        String currency,
        RentPeriod rentPeriod,
        String copy,
        Instant featuredUntil,
        PropertyStatus propertyStatus,
        String city,
        String suburb,
        String street,
        String typeLabel,
        Integer bedrooms,
        Integer bathrooms,
        Integer parkingSpaces,
        BigDecimal sizeSqm,
        List<PropertyResponse.MediaResponse> media,
        Instant createdAt,
        UUID cityId,
        UUID areaId,
        String reviewNote,
        UUID managingOrganisationId,
        String organisationName,
        boolean organisationVerified
) {}
