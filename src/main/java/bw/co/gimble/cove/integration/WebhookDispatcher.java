package bw.co.gimble.cove.integration;

import bw.co.gimble.cove.company.WebhookEndpoint;
import bw.co.gimble.cove.company.WebhookEndpointRepository;
import bw.co.gimble.cove.events.LeadCreatedEvent;
import bw.co.gimble.cove.events.ListingModeratedEvent;
import bw.co.gimble.cove.events.ListingPublishedEvent;
import bw.co.gimble.cove.events.ViewingBookedEvent;
import bw.co.gimble.cove.listings.Listing;
import bw.co.gimble.cove.listings.ListingRepository;
import bw.co.gimble.cove.properties.Property;
import bw.co.gimble.cove.properties.PropertyRepository;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Sends company webhooks. Each delivery is a POST with a JSON body
 * {id, event, createdAt, data} and headers X-Cove-Event, X-Cove-Delivery and
 * X-Cove-Signature: sha256=HMAC_SHA256(secret, body). Endpoints are disabled
 * after 10 consecutive failures; re-enabling resets the counter.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookDispatcher {

    private static final int MAX_FAILURES = 10;

    private final WebhookEndpointRepository webhookRepository;
    private final ListingRepository listingRepository;
    private final PropertyRepository propertyRepository;
    private final OutboundUrlPolicy urlPolicy;
    private final TransactionTemplate transactionTemplate;

    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    @PreDestroy
    void shutdown() {
        executor.shutdown();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onLead(LeadCreatedEvent e) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("leadId", e.leadId());
        data.put("listingId", e.listingId());
        forListing(e.listingId(), "lead.created", data);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onViewing(ViewingBookedEvent e) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("viewingId", e.viewingId());
        data.put("listingId", e.listingId());
        forListing(e.listingId(), "viewing.requested", data);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPublished(ListingPublishedEvent e) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("listingId", e.listingId());
        data.put("propertyId", e.propertyId());
        data.put("purpose", e.purpose().name());
        forListing(e.listingId(), "listing.published", data);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onModerated(ListingModeratedEvent e) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("listingId", e.listingId());
        data.put("status", e.status().name());
        data.put("reason", e.reason());
        String event = switch (e.status()) {
            case REJECTED -> "listing.rejected";
            case TAKEN_DOWN -> "listing.taken_down";
            default -> null;
        };
        if (event != null) forListing(e.listingId(), event, data);
    }

    /** Sends a ping synchronously so the portal can show the result. */
    public String sendTest(WebhookEndpoint hook) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("message", "Test delivery from Cove");
        return deliver(hook.getId(), hook.getUrl(), hook.getSecret(), "ping", data);
    }

    private void forListing(UUID listingId, String event, Map<String, Object> data) {
        try {
            UUID orgId = listingRepository.findById(listingId)
                    .map(Listing::getPropertyId)
                    .flatMap(propertyRepository::findById)
                    .map(Property::getManagingOrganisationId)
                    .orElse(null);
            if (orgId == null) return;
            listingRepository.findById(listingId).flatMap(l -> propertyRepository.findById(l.getPropertyId()))
                    .map(Property::getExternalRef)
                    .ifPresent(ref -> data.put("externalRef", ref));
            for (WebhookEndpoint hook : webhookRepository.findByOrganisationIdAndActiveTrue(orgId)) {
                if (!hook.wants(event)) continue;
                UUID id = hook.getId();
                String url = hook.getUrl();
                String secret = hook.getSecret();
                executor.submit(() -> deliver(id, url, secret, event, data));
            }
        } catch (Exception ex) {
            log.warn("Could not queue webhook {} for listing {}: {}", event, listingId, ex.getMessage());
        }
    }

    private String deliver(UUID hookId, String url, String secret, String event, Map<String, Object> data) {
        String deliveryId = UUID.randomUUID().toString();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", deliveryId);
        payload.put("event", event);
        payload.put("createdAt", Instant.now().toString());
        payload.put("data", data);
        String body = Json.write(payload);
        String status;
        boolean ok;
        try {
            HttpRequest request = HttpRequest.newBuilder(urlPolicy.require(url, true))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "Cove-Webhooks/1")
                    .header("X-Cove-Event", event)
                    .header("X-Cove-Delivery", deliveryId)
                    .header("X-Cove-Signature", "sha256=" + sign(secret, body))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<Void> response = http.send(request, HttpResponse.BodyHandlers.discarding());
            ok = response.statusCode() >= 200 && response.statusCode() < 300;
            status = "HTTP " + response.statusCode();
        } catch (Exception ex) {
            ok = false;
            status = "Failed: " + (ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
        }
        record(hookId, ok, status);
        return status;
    }

    private void record(UUID hookId, boolean ok, String status) {
        transactionTemplate.executeWithoutResult(tx -> webhookRepository.findById(hookId).ifPresent(h -> {
            h.setLastDeliveryAt(Instant.now());
            h.setLastStatus(status.length() > 250 ? status.substring(0, 250) : status);
            h.setConsecutiveFailures(ok ? 0 : h.getConsecutiveFailures() + 1);
            if (h.getConsecutiveFailures() >= MAX_FAILURES) {
                h.setActive(false);
                log.warn("Disabled webhook {} after {} consecutive failures", hookId, MAX_FAILURES);
            }
        }));
    }

    static String sign(String secret, String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
