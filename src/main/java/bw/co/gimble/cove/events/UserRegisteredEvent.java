package bw.co.gimble.cove.events;

import java.util.UUID;

public record UserRegisteredEvent(
        UUID userId,
        String email
) {}