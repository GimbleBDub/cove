package bw.co.gimble.cove.profile.dto.kyc;

import bw.co.gimble.cove.profile.enums.KycDocumentType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Sent as the "metadata" JSON part in a multipart request. The "file" part is optional here -
 * omit it to update metadata only and keep the previously uploaded document.
 */
public record UpdateKycRequest(

        @NotNull
        KycDocumentType documentType,

        String documentNumber,

        LocalDate issueDate,

        LocalDate expiryDate
) {
}