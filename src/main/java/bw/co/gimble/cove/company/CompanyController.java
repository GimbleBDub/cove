package bw.co.gimble.cove.company;

import bw.co.gimble.cove.common.ApiResponse;
import bw.co.gimble.cove.company.CompanyDtos.*;
import bw.co.gimble.cove.integration.WebhookDispatcher;
import bw.co.gimble.cove.listings.dto.ListingResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Company portal. Members can read; company admins (AGENCY_ADMIN) manage team, keys and webhooks. */
@RestController
@RequestMapping("/api/companies/{organisationId}")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;
    private final WebhookDispatcher webhookDispatcher;

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<OverviewResponse>> overview(@PathVariable UUID organisationId) {
        return ResponseEntity.ok(ApiResponse.ok(companyService.overview(organisationId)));
    }

    @GetMapping("/listings")
    public ResponseEntity<ApiResponse<List<ListingResponse>>> listings(@PathVariable UUID organisationId) {
        return ResponseEntity.ok(ApiResponse.ok(companyService.listings(organisationId)));
    }

    @GetMapping("/leads")
    public ResponseEntity<ApiResponse<List<CompanyLeadResponse>>> leads(
            @PathVariable UUID organisationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant since) {
        return ResponseEntity.ok(ApiResponse.ok(companyService.leads(organisationId, since)));
    }

    @GetMapping("/members")
    public ResponseEntity<ApiResponse<List<MemberResponse>>> members(@PathVariable UUID organisationId) {
        return ResponseEntity.ok(ApiResponse.ok(companyService.members(organisationId)));
    }

    @PostMapping("/members/invite")
    public ResponseEntity<ApiResponse<InviteMemberResponse>> invite(@PathVariable UUID organisationId,
                                                                    @Valid @RequestBody InviteMemberRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(companyService.invite(organisationId, request)));
    }

    @GetMapping("/api-keys")
    public ResponseEntity<ApiResponse<List<ApiKeyResponse>>> apiKeys(@PathVariable UUID organisationId) {
        return ResponseEntity.ok(ApiResponse.ok(companyService.apiKeys(organisationId)));
    }

    @PostMapping("/api-keys")
    public ResponseEntity<ApiResponse<ApiKeyResponse>> createApiKey(@PathVariable UUID organisationId,
                                                                    @Valid @RequestBody CreateApiKeyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(companyService.createApiKey(organisationId, request)));
    }

    @DeleteMapping("/api-keys/{keyId}")
    public ResponseEntity<Void> revokeApiKey(@PathVariable UUID organisationId, @PathVariable UUID keyId) {
        companyService.revokeApiKey(organisationId, keyId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/webhooks")
    public ResponseEntity<ApiResponse<List<WebhookResponse>>> webhooks(@PathVariable UUID organisationId) {
        return ResponseEntity.ok(ApiResponse.ok(companyService.webhooks(organisationId)));
    }

    @GetMapping("/webhooks/events")
    public ResponseEntity<ApiResponse<List<String>>> webhookEvents(@PathVariable UUID organisationId) {
        companyService.requireMember(organisationId);
        return ResponseEntity.ok(ApiResponse.ok(CompanyDtos.WEBHOOK_EVENTS));
    }

    @PostMapping("/webhooks")
    public ResponseEntity<ApiResponse<WebhookResponse>> createWebhook(@PathVariable UUID organisationId,
                                                                      @Valid @RequestBody WebhookRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(companyService.createWebhook(organisationId, request)));
    }

    @PutMapping("/webhooks/{webhookId}")
    public ResponseEntity<ApiResponse<WebhookResponse>> updateWebhook(@PathVariable UUID organisationId, @PathVariable UUID webhookId,
                                                                      @Valid @RequestBody WebhookRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(companyService.updateWebhook(organisationId, webhookId, request)));
    }

    @DeleteMapping("/webhooks/{webhookId}")
    public ResponseEntity<Void> deleteWebhook(@PathVariable UUID organisationId, @PathVariable UUID webhookId) {
        companyService.deleteWebhook(organisationId, webhookId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/webhooks/{webhookId}/test")
    public ResponseEntity<ApiResponse<Map<String, String>>> testWebhook(@PathVariable UUID organisationId, @PathVariable UUID webhookId) {
        companyService.requireAdmin(organisationId);
        WebhookEndpoint hook = companyService.requireWebhook(organisationId, webhookId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("result", webhookDispatcher.sendTest(hook))));
    }
}
