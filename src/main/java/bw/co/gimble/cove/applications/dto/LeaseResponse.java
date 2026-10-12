package bw.co.gimble.cove.applications.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record LeaseResponse(
        UUID id,
        UUID propertyId,
        UUID listingId,
        UUID applicationId,
        UUID tenantUserId,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal rentAmount,
        String currency,
        BigDecimal depositAmount
) {}
