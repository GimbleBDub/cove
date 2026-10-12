package bw.co.gimble.cove.properties.dto;

import bw.co.gimble.cove.properties.PropertyStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PropertyResponse(
        UUID id,
        UUID typeId,
        String typeCode,
        String typeLabel,
        PropertyStatus status,
        String description,
        Integer bedrooms,
        Integer bathrooms,
        Integer parkingSpaces,
        BigDecimal sizeSqm,
        String country,
        String city,
        String suburb,
        String street,
        UUID cityId,
        UUID areaId,
        String externalRef,
        BigDecimal latitude,
        BigDecimal longitude,
        UUID managingOrganisationId,
        List<OwnerResponse> owners,
        List<MediaResponse> media,
        Instant createdAt,
        Long version
) {
    public record OwnerResponse(UUID userId, String email, BigDecimal splitPercent) {}
    public record MediaResponse(UUID id, String url, int sortOrder, String type) {
        public static MediaResponse of(bw.co.gimble.cove.properties.PropertyMedia m) {
            return new MediaResponse(m.getId(), m.getUrl(), m.getSortOrder(), m.isVideo() ? "VIDEO" : "IMAGE");
        }
    }
}
