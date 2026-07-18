package bw.co.gimble.cove.security;

import lombok.Getter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;
import java.util.UUID;

/**
 * Deliberately thin - only identity, no roles baked in. Roles/permissions are
 * always resolved fresh from the DB via PermissionService, never trusted from
 * the token, so a revoked role takes effect immediately rather than at token expiry.
 */
@Getter
public class AuthPrincipal extends UsernamePasswordAuthenticationToken {

    private final UUID userId;
    private final String email;

    public AuthPrincipal(UUID userId, String email) {
        super(email, null, List.of());
        this.userId = userId;
        this.email = email;
    }
}