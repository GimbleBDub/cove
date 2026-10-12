package bw.co.gimble.cove.integration;

import bw.co.gimble.cove.common.ApiException;
import bw.co.gimble.cove.common.ErrorCode;
import bw.co.gimble.cove.integration.IntegrationDtos.*;
import bw.co.gimble.cove.listings.Listing;
import bw.co.gimble.cove.listings.ListingRepository;
import bw.co.gimble.cove.listings.ListingService;
import bw.co.gimble.cove.listings.ListingStatus;
import bw.co.gimble.cove.listings.dto.ListingResponse;
import bw.co.gimble.cove.locations.LocationService;
import bw.co.gimble.cove.organisations.OrgVerificationStatus;
import bw.co.gimble.cove.organisations.OrganisationRepository;
import bw.co.gimble.cove.properties.Property;
import bw.co.gimble.cove.properties.PropertyMedia;
import bw.co.gimble.cove.properties.PropertyMediaRepository;
import bw.co.gimble.cove.properties.PropertyRepository;
import bw.co.gimble.cove.properties.PropertyService;
import bw.co.gimble.cove.properties.PropertyType;
import bw.co.gimble.cove.properties.PropertyTypeRepository;
import bw.co.gimble.cove.properties.dto.CreatePropertyRequest;
import bw.co.gimble.cove.properties.dto.UpdatePropertyRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Turns a company's ERP record into a Cove property + listing. Upserts are
 * idempotent per externalRef, and atomic: if anything fails (e.g. a media URL
 * can't be fetched, or publishing needs a photo) nothing is saved.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class IntegrationService {

    private static final Pattern REF = Pattern.compile("[A-Za-z0-9._-]{1,100}");

    private final PropertyRepository propertyRepository;
    private final PropertyTypeRepository typeRepository;
    private final PropertyService propertyService;
    private final PropertyMediaRepository mediaRepository;
    private final ListingService listingService;
    private final ListingRepository listingRepository;
    private final LocationService locationService;
    private final OrganisationRepository organisationRepository;
    private final MediaImporter mediaImporter;

    @Transactional(readOnly = true)
    public WhoAmIResponse whoAmI(IntegrationContext ctx) {
        var org = organisationRepository.findById(ctx.organisationId()).orElseThrow();
        return new WhoAmIResponse(org.getId(), org.getName(), org.getVerificationStatus().name(), ctx.apiKeyName(),
                org.getVerificationStatus() == OrgVerificationStatus.VERIFIED);
    }

    @Transactional(readOnly = true)
    public List<IntegrationListingResponse> list(IntegrationContext ctx) {
        return propertyRepository.findByManagingOrganisationId(ctx.organisationId()).stream()
                .sorted(Comparator.comparing(Property::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public IntegrationListingResponse get(IntegrationContext ctx, String externalRef) {
        return toResponse(requireByRef(ctx, externalRef));
    }

    public IntegrationListingResponse upsert(IntegrationContext ctx, String externalRef, UpsertListingRequest req) {
        if (!REF.matcher(externalRef).matches()) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED,
                    "externalRef must be 1-100 characters of letters, digits, '.', '_' or '-'");
        }
        PropertyType type = typeRepository.findByCodeIgnoreCase(req.propertyType().trim())
                .filter(PropertyType::isActive)
                .orElseThrow(() -> ApiException.badRequest(ErrorCode.VALIDATION_FAILED,
                        "Unknown propertyType '" + req.propertyType() + "'. See GET /reference/property-types"));
        LocationService.Place place = req.cityId() != null || req.areaId() != null
                ? locationService.resolve(req.cityId(), req.areaId())
                : locationService.resolveByName(req.city(), req.area());

        Property property = propertyRepository
                .findByManagingOrganisationIdAndExternalRef(ctx.organisationId(), externalRef).orElse(null);
        if (property == null) {
            var created = propertyService.create(new CreatePropertyRequest(
                    type.getId(), req.description(), req.bedrooms(), req.bathrooms(), req.parkingSpaces(), req.sizeSqm(),
                    "BW", place.city().getId(), place.area().getId(), null, null, req.street(), null, null,
                    ctx.organisationId(), null));
            property = propertyRepository.findById(created.id()).orElseThrow();
            property.setExternalRef(externalRef);
        } else {
            propertyService.update(property.getId(), new UpdatePropertyRequest(
                    type.getId(), req.description(), req.bedrooms(), req.bathrooms(), req.parkingSpaces(), req.sizeSqm(),
                    "BW", place.city().getId(), place.area().getId(), null, null, req.street(), null, null, null));
        }

        if (req.mediaUrls() != null) syncMedia(property, req.mediaUrls());

        boolean publish = req.publish() == null || req.publish();
        if (publish) {
            listingService.upsertAndPublish(new bw.co.gimble.cove.listings.dto.UpsertListingRequest(
                    property.getId(), req.purpose(), req.price(), "BWP", req.rentPeriod(), req.description(), null));
            // A record switched from rent to sale (or back) shouldn't stay listed both ways.
            for (Listing other : listingRepository.findByPropertyIdIn(List.of(property.getId()))) {
                if (other.getPurpose() != req.purpose() && isActive(other)) listingService.unpublish(other.getId());
            }
        } else {
            withdrawAll(property);
        }
        return toResponse(property);
    }

    public IntegrationListingResponse withdraw(IntegrationContext ctx, String externalRef) {
        Property property = requireByRef(ctx, externalRef);
        withdrawAll(property);
        return toResponse(property);
    }

    private void withdrawAll(Property property) {
        for (Listing l : listingRepository.findByPropertyIdIn(List.of(property.getId()))) {
            if (isActive(l)) listingService.unpublish(l.getId());
        }
    }

    private static boolean isActive(Listing l) {
        return l.getStatus() == ListingStatus.PUBLISHED || l.getStatus() == ListingStatus.PENDING_REVIEW;
    }

    /** Media imported by URL mirrors [urls]: new ones are fetched, missing ones removed, order follows the list. */
    private void syncMedia(Property property, List<String> urls) {
        List<String> wanted = urls.stream().map(String::trim).distinct().toList();
        Map<String, PropertyMedia> imported = new HashMap<>();
        for (PropertyMedia m : mediaRepository.findByPropertyIdOrderBySortOrderAscCreatedAtAsc(property.getId())) {
            if (m.getSourceUrl() == null) continue;
            if (wanted.contains(m.getSourceUrl())) imported.put(m.getSourceUrl(), m);
            else propertyService.deleteMedia(property.getId(), m.getId());
        }
        for (String url : wanted) {
            if (imported.containsKey(url)) continue;
            propertyService.addMedia(property.getId(), mediaImporter.download(url), url);
        }
        Map<String, UUID> idsBySource = new HashMap<>();
        List<UUID> manual = new ArrayList<>();
        for (PropertyMedia m : mediaRepository.findByPropertyIdOrderBySortOrderAscCreatedAtAsc(property.getId())) {
            if (m.getSourceUrl() != null) idsBySource.put(m.getSourceUrl(), m.getId());
            else manual.add(m.getId());
        }
        List<UUID> order = new ArrayList<>();
        wanted.forEach(u -> { if (idsBySource.containsKey(u)) order.add(idsBySource.get(u)); });
        order.addAll(manual);
        if (!order.isEmpty()) propertyService.reorderMedia(property.getId(), order);
    }

    private Property requireByRef(IntegrationContext ctx, String externalRef) {
        return propertyRepository.findByManagingOrganisationIdAndExternalRef(ctx.organisationId(), externalRef)
                .orElseThrow(() -> ApiException.notFound("No listing with externalRef '" + externalRef + "'"));
    }

    private IntegrationListingResponse toResponse(Property p) {
        List<ListingResponse> listings = listingRepository.findByPropertyIdIn(List.of(p.getId())).stream()
                .map(listingService::toResponse).toList();
        return new IntegrationListingResponse(p.getExternalRef(), p.getId(), p.getStatus().name(), listings);
    }
}
