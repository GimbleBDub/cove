package bw.co.gimble.cove.listings.dto;

import bw.co.gimble.cove.listings.ListingPurpose;
import bw.co.gimble.cove.listings.RentPeriod;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record UpsertListingRequest(
        @NotNull UUID propertyId,
        @NotNull ListingPurpose purpose,
        @NotNull(message = "Enter a price")
        @jakarta.validation.constraints.DecimalMin(value = "1", message = "Price must be greater than zero")
        @jakarta.validation.constraints.DecimalMax(value = "1000000000", message = "That price looks too high")
        @jakarta.validation.constraints.Digits(integer = 12, fraction = 2, message = "Use at most 2 decimal places")
        BigDecimal price,
        String currency,
        RentPeriod rentPeriod,
        @jakarta.validation.constraints.Size(max = 4000, message = "The description can be at most 4000 characters") String copy,
        @jakarta.validation.constraints.Min(0) @jakarta.validation.constraints.Max(90) Integer featuredDays
) {}
