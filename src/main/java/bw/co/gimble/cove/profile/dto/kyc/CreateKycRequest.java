package bw.co.gimble.cove.profile.dto.kyc;

import bw.co.gimble.cove.profile.enums.KycDocumentType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateKycRequest(

        @NotNull
        KycDocumentType documentType,

        String documentNumber,

        LocalDate issueDate,

        LocalDate expiryDate
) {
}