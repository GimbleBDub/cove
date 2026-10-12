package bw.co.gimble.cove.integration;

import bw.co.gimble.cove.company.ApiKey;
import bw.co.gimble.cove.company.ApiKeyRepository;
import bw.co.gimble.cove.permission.PermissionService;
import bw.co.gimble.cove.role.Role;
import bw.co.gimble.cove.security.AuthPrincipal;
import bw.co.gimble.cove.security.TokenHasher;
import bw.co.gimble.cove.user.UserRepository;
import bw.co.gimble.cove.user.UserStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;

/**
 * Authenticates /api/v1/integration/** with a company API key
 * (X-Api-Key: cove_live_{prefix}_{secret}, or Authorization: Bearer ...).
 * The call then runs as the admin who created the key, scoped to that company.
 * Keys stop working if revoked or if their creator is no longer a company admin.
 */
@Component
@RequiredArgsConstructor
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    public static final String PATH_PREFIX = "/api/v1/integration";

    private final ApiKeyRepository apiKeyRepository;
    private final UserRepository userRepository;
    private final PermissionService permissionService;
    private final TokenHasher tokenHasher;
    private final TransactionTemplate transactionTemplate;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getServletPath().startsWith(PATH_PREFIX) || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // A user JWT is never enough here; only API keys reach integration endpoints.
        SecurityContextHolder.clearContext();
        String raw = request.getHeader("X-Api-Key");
        if (raw == null) {
            String auth = request.getHeader("Authorization");
            if (auth != null && auth.startsWith("Bearer cove_")) raw = auth.substring(7);
        }
        if (raw == null || !raw.startsWith("cove_live_")) {
            reject(response, "Send your API key in the X-Api-Key header");
            return;
        }
        String[] parts = raw.substring("cove_live_".length()).split("_", 2);
        if (parts.length != 2) {
            reject(response, "Malformed API key");
            return;
        }
        ApiKey key = apiKeyRepository.findByPrefix(parts[0]).orElse(null);
        if (key == null || key.getRevokedAt() != null
                || !MessageDigest.isEqual(key.getSecretHash().getBytes(StandardCharsets.UTF_8),
                tokenHasher.hash(parts[1]).getBytes(StandardCharsets.UTF_8))) {
            reject(response, "Invalid or revoked API key");
            return;
        }
        var owner = userRepository.findById(key.getCreatedByUserId()).orElse(null);
        if (owner == null || owner.getStatus() != UserStatus.ACTIVE
                || !permissionService.hasOrgRole(owner.getId(), key.getOrganisationId(), Role.AGENCY_ADMIN)) {
            reject(response, "The admin who created this key no longer has access. Create a new key.");
            return;
        }
        SecurityContextHolder.getContext().setAuthentication(new AuthPrincipal(owner.getId(), owner.getEmail()));
        request.setAttribute(IntegrationContext.ATTRIBUTE,
                new IntegrationContext(key.getOrganisationId(), key.getId(), key.getName()));
        touch(key);
        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void touch(ApiKey key) {
        Instant last = key.getLastUsedAt();
        if (last != null && last.isAfter(Instant.now().minus(Duration.ofMinutes(1)))) return;
        transactionTemplate.executeWithoutResult(tx ->
                apiKeyRepository.findById(key.getId()).ifPresent(k -> k.setLastUsedAt(Instant.now())));
    }

    private void reject(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"success\":false,\"error\":{\"code\":\"API_KEY_INVALID\",\"message\":\""
                + message.replace("\"", "'") + "\"}}");
    }
}
