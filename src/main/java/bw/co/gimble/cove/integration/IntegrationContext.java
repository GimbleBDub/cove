package bw.co.gimble.cove.integration;

import java.util.UUID;

/** Set on the request by [ApiKeyAuthFilter]; read by the integration controller. */
public record IntegrationContext(UUID organisationId, UUID apiKeyId, String apiKeyName) {
    public static final String ATTRIBUTE = "cove.integration";
}
