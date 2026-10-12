package bw.co.gimble.cove.integration;

import bw.co.gimble.cove.listings.ListingPurpose;
import bw.co.gimble.cove.listings.RentPeriod;
import bw.co.gimble.cove.listings.dto.ListingResponse;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class IntegrationDtos {
    private IntegrationDtos() {}

    /**
     * One property and its listing, keyed by the company's own reference.
     * Location: either cityId+areaId, or city+area names exactly as in GET /reference/locations.
     */
    public record UpsertListingRequest(
            @NotBlank String propertyType,
            UUID cityId,
            UUID areaId,
            String city,
            String area,
            @Size(max = 255) String street,
            @Min(0) @Max(100) Integer bedrooms,
            @Min(0) @Max(100) Integer bathrooms,
            @Min(0) @Max(100) Integer parkingSpaces,
            @DecimalMin("0") BigDecimal sizeSqm,
            @Size(max = 4000) String description,
            @NotNull ListingPurpose purpose,
            @NotNull @DecimalMin(value = "1", message = "price must be greater than zero") BigDecimal price,
            RentPeriod rentPeriod,
            @Size(max = 40) List<@NotBlank String> mediaUrls,
            Boolean publish
    ) {}

    public record IntegrationListingResponse(
            String externalRef,
            UUID propertyId,
            String propertyStatus,
            List<ListingResponse> listings
    ) {}

    public record WhoAmIResponse(UUID organisationId, String organisationName, String verificationStatus,
                                 String apiKeyName, boolean listingsGoLiveImmediately) {}
}
