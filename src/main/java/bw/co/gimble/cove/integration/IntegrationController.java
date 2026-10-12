package bw.co.gimble.cove.integration;

import bw.co.gimble.cove.common.ApiResponse;
import bw.co.gimble.cove.company.CompanyDtos.CompanyLeadResponse;
import bw.co.gimble.cove.company.CompanyService;
import bw.co.gimble.cove.integration.IntegrationDtos.*;
import bw.co.gimble.cove.locations.LocationService;
import bw.co.gimble.cove.locations.dto.LocationDtos.CityResponse;
import bw.co.gimble.cove.properties.PropertyTypeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/**
 * Public integration API for companies' own systems (ERP, CRM, websites).
 * Authenticate with the X-Api-Key header. Versioned under /api/v1.
 */
@RestController
@RequestMapping(ApiKeyAuthFilter.PATH_PREFIX)
@RequiredArgsConstructor
public class IntegrationController {

    private final IntegrationService integrationService;
    private final CompanyService companyService;
    private final LocationService locationService;
    private final PropertyTypeService propertyTypeService;

    private static IntegrationContext ctx(HttpServletRequest request) {
        return (IntegrationContext) request.getAttribute(IntegrationContext.ATTRIBUTE);
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<WhoAmIResponse>> me(HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(integrationService.whoAmI(ctx(request))));
    }

    @GetMapping("/reference/property-types")
    public ResponseEntity<ApiResponse<?>> propertyTypes() {
        return ResponseEntity.ok(ApiResponse.ok(propertyTypeService.listActive()));
    }

    @GetMapping("/reference/locations")
    public ResponseEntity<ApiResponse<List<CityResponse>>> locations() {
        return ResponseEntity.ok(ApiResponse.ok(locationService.publicCatalogue()));
    }

    @GetMapping("/listings")
    public ResponseEntity<ApiResponse<List<IntegrationListingResponse>>> listings(HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(integrationService.list(ctx(request))));
    }

    @GetMapping("/listings/{externalRef}")
    public ResponseEntity<ApiResponse<IntegrationListingResponse>> get(HttpServletRequest request, @PathVariable String externalRef) {
        return ResponseEntity.ok(ApiResponse.ok(integrationService.get(ctx(request), externalRef)));
    }

    /** Create or update by your own reference. Safe to call repeatedly. */
    @PutMapping("/listings/{externalRef}")
    public ResponseEntity<ApiResponse<IntegrationListingResponse>> upsert(HttpServletRequest request, @PathVariable String externalRef,
                                                                          @Valid @RequestBody UpsertListingRequest body) {
        return ResponseEntity.ok(ApiResponse.ok(integrationService.upsert(ctx(request), externalRef, body)));
    }

    /** Takes the listing off the marketplace (the property record is kept). */
    @DeleteMapping("/listings/{externalRef}")
    public ResponseEntity<ApiResponse<IntegrationListingResponse>> withdraw(HttpServletRequest request, @PathVariable String externalRef) {
        return ResponseEntity.ok(ApiResponse.ok(integrationService.withdraw(ctx(request), externalRef)));
    }

    /** Enquiries on your listings, newest first. Poll with ?since= or subscribe to the lead.created webhook. */
    @GetMapping("/leads")
    public ResponseEntity<ApiResponse<List<CompanyLeadResponse>>> leads(
            HttpServletRequest request,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant since) {
        return ResponseEntity.ok(ApiResponse.ok(companyService.leadResponses(ctx(request).organisationId(), since)));
    }
}
