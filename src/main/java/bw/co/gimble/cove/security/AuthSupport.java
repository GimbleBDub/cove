package bw.co.gimble.cove.security;

import bw.co.gimble.cove.common.ApiException;
import bw.co.gimble.cove.common.ErrorCode;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public final class AuthSupport {

    private AuthSupport() {}

    public static UUID currentUserIdOrNull() {
        Object auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof AuthPrincipal principal) {
            return principal.getUserId();
        }
        return null;
    }

    public static UUID requireUserId() {
        UUID id = currentUserIdOrNull();
        if (id == null) {
            throw ApiException.unauthorized(ErrorCode.AUTH_TOKEN_INVALID, "Not authenticated");
        }
        return id;
    }
}
