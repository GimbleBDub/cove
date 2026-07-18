package bw.co.gimble.cove.events;

import bw.co.gimble.cove.profile.enums.KycDocumentType;

import java.util.UUID;

public record KycDocumentSubmittedEvent(
        UUID kycId, UUID userId,
        KycDocumentType documentType
) {}