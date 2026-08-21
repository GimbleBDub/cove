package bw.co.gimble.cove.security;

import bw.co.gimble.cove.profile.repositories.UserProfileRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Enforces "profile is mandatory": once a request carries a valid JWT, every
 * endpoint except the exempt prefixes below 403s with PROFILE_INCOMPLETE
 * until a UserProfile row exists for that user.
 *
 * Deliberately does NOT gate on account-type (individual/org) or KYC - those
 * stay optional/flexible per product spec. This filter only enforces the
 * one hard requirement: basic profile info.
 *
 * Writes the error body manually (no ObjectMapper dependency) to avoid
 * coupling this low-level security filter to Jackson bean wiring.
 */
@Component
public class ProfileGateFilter extends OncePerRequestFilter {

    private final UserProfileRepository userProfileRepository;

    public ProfileGateFilter(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    // Must stay reachable pre-gate, or a new user could never satisfy it.
    private static final List<String> EXEMPT_PREFIXES = List.of(
            "/api/profile",
            "/api/auth"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        Object auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof AuthPrincipal principal) || isExempt(request.getServletPath())) {
            chain.doFilter(request, response);
            return;
        }

        if (!userProfileRepository.existsByUserId(principal.getUserId())) {
            writeProfileIncomplete(response);
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isExempt(String path) {
        return EXEMPT_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private void writeProfileIncomplete(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String json = """
                {"success":false,"error":{"code":"PROFILE_INCOMPLETE","message":"Complete your profile before continuing"}}
                """;
        response.getWriter().write(json.strip());
    }
}