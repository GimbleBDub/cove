package bw.co.gimble.cove.properties.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OwnerSplitRequest(UUID userId, BigDecimal splitPercent) {}
