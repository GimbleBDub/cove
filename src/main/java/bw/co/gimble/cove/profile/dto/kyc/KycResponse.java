package bw.co.gimble.cove.profile.dto.kyc;

import bw.co.gimble.cove.profile.enums.KycDocumentType;
import bw.co.gimble.cove.profile.enums.KycStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record KycResponse(

        UUID id,

        KycDocumentType documentType,

        KycStatus status,

        String fileUrl,

        String documentNumber,

        LocalDate issueDate,

        LocalDate expiryDate,

        LocalDate verifiedAt,

        LocalDate rejectedAt,

        String rejectionReason,

        String verifiedBy,

        Instant createdAt,

        Instant updatedAt,

        // Who submitted it - shown in the admin review queue.
        String userEmail
) {
}
