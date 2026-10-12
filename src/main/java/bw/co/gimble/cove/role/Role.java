package bw.co.gimble.cove.role;

/**
 * requiresOrganisation = true  -> this role only makes sense tied to an Organisation
 *                                  (organisationId must NOT be null on the UserRole row)
 * requiresOrganisation = false -> personal-capacity role (organisationId must be null)
 *
 * PLATFORM_ADMIN is global (no organisations) and is never grantable via the normal
 * organisations-admin assignment flow - it must go through a separate, more guarded path.
 */
public enum Role {

    PLATFORM_ADMIN(false),

    AGENCY_ADMIN(true),
    PROPERTY_MANAGER(true),
    AGENT(true),
    ORG_STAFF(true),
    VALUER(true),
    AUCTIONEER(true),

    OWNER(false),
    TENANT(false),
    BUYER(false);

    private final boolean requiresOrganisation;

    Role(boolean requiresOrganisation) {
        this.requiresOrganisation = requiresOrganisation;
    }

    public boolean requiresOrganisation() {
        return requiresOrganisation;
    }
}