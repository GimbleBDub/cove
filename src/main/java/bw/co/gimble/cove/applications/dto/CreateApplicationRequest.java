package bw.co.gimble.cove.applications.dto;

import bw.co.gimble.cove.applications.ApplicationType;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateApplicationRequest(
        @NotNull UUID listingId,
        @NotNull ApplicationType type,
        Integer occupants,
        BigDecimal monthlyIncome,
        BigDecimal offerAmount,
        String conditions,
        String referralSource,
        LocalDate proposedStartDate,
        LocalDate proposedEndDate,
        BigDecimal proposedRent,
        BigDecimal proposedDeposit
) {}
