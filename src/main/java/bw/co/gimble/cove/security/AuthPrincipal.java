package bw.co.gimble.cove.security;

import lombok.Getter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;
import java.util.UUID;

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