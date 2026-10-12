package bw.co.gimble.cove.admin;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AdminDtos {
    private AdminDtos() {}

    public record DashboardResponse(
            long users,
            long newUsersLast7Days,
            long companies,
            long companiesAwaitingVerification,
            long kycAwaitingReview,
            long listingsAwaitingReview,
            long liveListings,
            long properties,
            long enquiriesLast30Days,
            long viewingsLast30Days
    ) {}

    public record AdminUserResponse(
            UUID id,
            String email,
            String phone,
            String name,
            String status,
            boolean emailVerified,
            boolean staff,
            List<String> roles,
            List<String> companies,
            long kycVerified,
            long kycPending,
            Instant createdAt
    ) {}

    public record AdminOrganisationResponse(
            UUID id,
            String name,
            List<String> types,
            String verificationStatus,
            String email,
            String phone,
            long members,
            long properties,
            long liveListings,
            Instant createdAt
    ) {}

    public record AuditEntryResponse(
            UUID id,
            String entityType,
            UUID entityId,
            String action,
            String fromValue,
            String toValue,
            UUID actorUserId,
            String actorEmail,
            String note,
            Instant occurredAt
    ) {}

    public record ReasonRequest(String reason) {}
}
