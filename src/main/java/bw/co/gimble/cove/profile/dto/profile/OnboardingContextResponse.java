package bw.co.gimble.cove.profile.dto.profile;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Who the signed-in user is, for routing the client to the right experience
 * (consumer app, Cove back office, company portal). Served under /api/profile
 * so it stays reachable before the profile gate is satisfied.
 */
public record OnboardingContextResponse(
        boolean profileCompleted,
        boolean platformAdmin,
        String email,
        Set<String> staffPermissions,
        boolean passwordChangeRequired,
        String displayName,
        List<OrganisationMembership> organisations
) {
    public record OrganisationMembership(
            UUID id,
            String name,
            String verificationStatus,
            List<String> roles,
            boolean admin
    ) {}
}
