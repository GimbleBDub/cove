package bw.co.gimble.cove.profile.dto.kyc;

public record KycSummaryResponse(

        int totalDocuments,

        int verified,

        int pending,

        int rejected,

        int expired,

        boolean complete
) {
}
