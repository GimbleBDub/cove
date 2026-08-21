package bw.co.gimble.cove.profile.dto.kyc;

/** rejectionReason is only used when calling the /reject endpoint. */
public record ReviewKycRequest(
        String rejectionReason
) {}