package bw.co.gimble.cove.applications.dto;

import bw.co.gimble.cove.applications.ApplicationStatus;
import bw.co.gimble.cove.applications.ApplicationType;
import bw.co.gimble.cove.profile.enums.KycDocumentType;
import bw.co.gimble.cove.profile.enums.KycStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ApplicationResponse(
        UUID id,
        UUID listingId,
        UUID applicantUserId,
        ApplicationType type,
        ApplicationStatus status,
        Integer occupants,
        BigDecimal monthlyIncome,
        BigDecimal offerAmount,
        String conditions,
        String referralSource,
        LocalDate proposedStartDate,
        LocalDate proposedEndDate,
        BigDecimal proposedRent,
        BigDecimal proposedDeposit,
        List<ApplicantKyc> applicantKyc,
        List<SignatureResponse> signatures,
        Instant createdAt
) {
    public record ApplicantKyc(KycDocumentType documentType, KycStatus status, String documentNumber) {}
    public record SignatureResponse(UUID id, UUID signerUserId, String party, String typedName, Instant createdAt) {}
}
