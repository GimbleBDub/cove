package bw.co.gimble.cove.company;

import bw.co.gimble.cove.role.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class CompanyDtos {
    private CompanyDtos() {}

    /** Every event a webhook can subscribe to. */
    public static final List<String> WEBHOOK_EVENTS = List.of(
            "lead.created", "viewing.requested", "listing.published", "listing.rejected", "listing.taken_down");

    public record OverviewResponse(
            UUID id, String name, String verificationStatus, String logoUrl, List<String> types,
            boolean isAdmin, long members, long properties, long liveListings, long pendingListings,
            long rejectedListings, long enquiriesLast30Days, long newEnquiries
    ) {}

    public record CompanyLeadResponse(
            UUID id, UUID listingId, String listingTitle, String inquirerName, String inquirerEmail,
            String inquirerPhone, String stage, UUID assignedAgentUserId, Instant createdAt
    ) {}

    public record MemberRole(UUID roleId, String role) {}

    public record MemberResponse(UUID userId, String name, String email, String phone, List<MemberRole> roles, Instant joinedAt) {}

    public record InviteMemberRequest(
            @NotBlank String firstName, @NotBlank String lastName,
            @NotBlank @Email String email, @NotBlank String phone, @NotNull Role role
    ) {}

    public record InviteMemberResponse(UUID userId, String email, String role, String temporaryPassword) {}

    public record CreateApiKeyRequest(@NotBlank @Size(max = 80) String name) {}

    /** [key] is only returned at creation. */
    public record ApiKeyResponse(UUID id, String name, String prefix, String createdBy, Instant createdAt,
                                 Instant lastUsedAt, boolean revoked, String key) {}

    public record WebhookRequest(@NotBlank @Size(max = 1000) String url, Set<String> events, Boolean active) {}

    /** [secret] is only returned at creation. */
    public record WebhookResponse(UUID id, String url, List<String> events, boolean active, Instant lastDeliveryAt,
                                  String lastStatus, int consecutiveFailures, Instant createdAt, String secret) {}
}
