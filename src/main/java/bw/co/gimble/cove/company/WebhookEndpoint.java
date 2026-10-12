package bw.co.gimble.cove.company;

import bw.co.gimble.cove.common.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/** Where Cove POSTs signed event notifications for a company (lead.created etc.). */
@Getter
@Setter
@Entity
@Table(name = "webhook_endpoints")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookEndpoint extends Auditable {

    @Column(name = "organisation_id", nullable = false)
    private UUID organisationId;

    @Column(nullable = false, length = 1000)
    private String url;

    /** HMAC-SHA256 signing secret, shown to the company once. */
    @Column(nullable = false)
    private String secret;

    /** Comma-separated event names, or "*" for all. */
    @Column(nullable = false, length = 1000)
    @Builder.Default
    private String events = "*";

    @Column(nullable = false, columnDefinition = "boolean not null default true")
    @Builder.Default
    private boolean active = true;

    @Column(name = "last_delivery_at")
    private Instant lastDeliveryAt;

    @Column(name = "last_status")
    private String lastStatus;

    @Column(name = "consecutive_failures", nullable = false, columnDefinition = "integer not null default 0")
    @Builder.Default
    private int consecutiveFailures = 0;

    public boolean wants(String event) {
        if ("*".equals(events.trim())) return true;
        for (String e : events.split(",")) {
            if (e.trim().equalsIgnoreCase(event)) return true;
        }
        return false;
    }
}
