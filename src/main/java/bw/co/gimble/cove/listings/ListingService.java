package bw.co.gimble.cove.listings;

import bw.co.gimble.cove.staff.StaffPermission;
import bw.co.gimble.cove.common.ApiException;
import bw.co.gimble.cove.common.AuditLogService;
import bw.co.gimble.cove.common.ErrorCode;
import bw.co.gimble.cove.events.ListingPublishedEvent;
import bw.co.gimble.cove.listings.dto.ListingResponse;
import bw.co.gimble.cove.listings.dto.SharePackResponse;
import bw.co.gimble.cove.listings.dto.UpsertListingRequest;
import bw.co.gimble.cove.properties.Property;
import bw.co.gimble.cove.properties.PropertyMediaRepository;
import bw.co.gimble.cove.properties.PropertyService;
import bw.co.gimble.cove.properties.PropertyType;
import bw.co.gimble.cove.properties.PropertyTypeRepository;
import bw.co.gimble.cove.properties.dto.PropertyResponse;
import bw.co.gimble.cove.security.AuthSupport;
import bw.co.gimble.cove.subscriptions.SubscriptionFunction;
import bw.co.gimble.cove.subscriptions.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ListingService {

    private final ListingRepository listingRepository;
    private final PropertyService propertyService;
    private final PropertyMediaRepository mediaRepository;
    private final PropertyTypeRepository typeRepository;
    private final SubscriptionService subscriptionService;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher eventPublisher;
    private final bw.co.gimble.cove.organisations.OrganisationRepository organisationRepository;
    private final bw.co.gimble.cove.permission.PermissionService permissionService;
    private final bw.co.gimble.cove.notifications.NotificationService notificationService;

    @Value("${app.base-url:http://localhost:64045}")
    private String appBaseUrl;

    public ListingResponse upsertAndPublish(UpsertListingRequest request) {
        UUID actor = AuthSupport.requireUserId();
        propertyService.requireNotStaff(actor);
        Property property = propertyService.requireManageable(request.propertyId(), actor);
        propertyService.requirePublishReady(property);
        if (property.getManagingOrganisationId() != null && !propertyService.isOwner(property.getId(), actor)) {
            propertyService.requireAgencyOnOrg(actor, property.getManagingOrganisationId());
        }
        if (request.purpose() == ListingPurpose.RENT && request.rentPeriod() == null) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "Rent listings need a rent period");
        }

        Listing listing = listingRepository.findByPropertyIdAndPurpose(property.getId(), request.purpose())
                .orElseGet(() -> Listing.builder()
                        .propertyId(property.getId())
                        .purpose(request.purpose())
                        .listedByUserId(actor)
                        .build());

        if (!"BWP".equalsIgnoreCase(request.currency() == null || request.currency().isBlank() ? "BWP" : request.currency())) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "Only BWP prices are supported for now");
        }
        listing.setPrice(request.price());
        listing.setCurrency("BWP");
        listing.setRentPeriod(request.purpose() == ListingPurpose.RENT ? request.rentPeriod() : null);
        listing.setCopy(request.copy() == null || request.copy().isBlank() ? property.getDescription() : request.copy().trim());
        listing.setListedByUserId(actor);
        applyFeatured(listing, actor, request.featuredDays());

        // Verified companies go live straight away; everyone else waits for a moderator.
        // Edits to an already-live listing stay live; anything a moderator took down goes back to review.
        ListingStatus previous = listing.getId() == null ? null : listing.getStatus();
        boolean goLive = previous == ListingStatus.PUBLISHED
                || (previous != ListingStatus.TAKEN_DOWN && isVerifiedCompanyListing(property));
        listing.setPublished(goLive);
        listing.setStatus(goLive ? ListingStatus.PUBLISHED : ListingStatus.PENDING_REVIEW);
        if (!goLive) {
            listing.setReviewNote(null);
            listing.setReviewedAt(null);
            listing.setReviewedByUserId(null);
        }
        listing = listingRepository.save(listing);
        if (goLive) {
            propertyService.ensureListed(property, actor);
            auditLogService.record("LISTING", listing.getId(), "PUBLISHED", previous == null ? null : previous.name(), ListingStatus.PUBLISHED.name(), actor, request.purpose().name());
            if (previous != ListingStatus.PUBLISHED) {
                eventPublisher.publishEvent(new ListingPublishedEvent(listing.getId(), property.getId(), request.purpose(), actor));
            }
        } else {
            auditLogService.record("LISTING", listing.getId(), "SUBMITTED_FOR_REVIEW", previous == null ? null : previous.name(), ListingStatus.PENDING_REVIEW.name(), actor, request.purpose().name());
        }
        return map(listing, property);
    }

    public ListingResponse unpublish(UUID listingId) {
        UUID actor = AuthSupport.requireUserId();
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> ApiException.notFound(ErrorCode.LISTING_NOT_FOUND, "Listing not found"));
        Property property = propertyService.requireManageable(listing.getPropertyId(), actor);
        if (listing.getStatus() == ListingStatus.CLOSED || listing.getStatus() == ListingStatus.TAKEN_DOWN) {
            throw ApiException.conflict(ErrorCode.INVALID_PROPERTY_STATUS_TRANSITION, "This listing cannot be unpublished");
        }
        ListingStatus previousStatus = listing.getStatus();
        listing.setPublished(false);
        listing.setStatus(ListingStatus.UNPUBLISHED);
        long remaining = listingRepository.countByPropertyIdAndPublishedTrueAndStatus(property.getId(), ListingStatus.PUBLISHED);
        propertyService.maybeUnlist(property, actor, remaining);
        auditLogService.record("LISTING", listing.getId(), "UNPUBLISHED", previousStatus.name(), ListingStatus.UNPUBLISHED.name(), actor, null);
        return map(listing, property);
    }

    public void close(Listing listing, UUID actor, String note) {
        listing.setPublished(false);
        listing.setStatus(ListingStatus.CLOSED);
        auditLogService.record("LISTING", listing.getId(), "CLOSED", ListingStatus.PUBLISHED.name(), ListingStatus.CLOSED.name(), actor, note);
    }

    @Transactional(readOnly = true)
    public ListingResponse get(UUID id) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(ErrorCode.LISTING_NOT_FOUND, "Listing not found"));
        Property property = propertyService.getEntity(listing.getPropertyId());
        if (!listing.isPublished() || listing.getStatus() != ListingStatus.PUBLISHED) {
            UUID actor = AuthSupport.currentUserIdOrNull();
            if (actor == null || !propertyService.canView(property, actor)) {
                throw ApiException.notFound(ErrorCode.LISTING_NOT_FOUND, "Listing not found");
            }
        }
        return map(listing, property);
    }

    @Transactional(readOnly = true)
    public List<ListingResponse> search(ListingPurpose purpose, UUID typeId, String city, String suburb,
                                        BigDecimal minPrice, BigDecimal maxPrice, boolean featuredOnly,
                                        Integer minBedrooms, BigDecimal minSizeSqm, BigDecimal maxSizeSqm,
                                        ListingSort sort, UUID cityId, UUID areaId) {
        String cityQ = blankToNull(city);
        String suburbQ = blankToNull(suburb);
        Instant now = Instant.now();
        ListingSort effectiveSort = sort == null ? ListingSort.FEATURED : sort;

        List<Listing> published = listingRepository.findByPublishedTrueAndStatusOrderByCreatedAtDesc(ListingStatus.PUBLISHED);
        Map<UUID, Property> properties = propertyService.getEntities(
                published.stream().map(Listing::getPropertyId).distinct().toList());

        List<ListingResponse> results = published.stream()
                .map(l -> {
                    Property property = properties.get(l.getPropertyId());
                    if (property == null) return null;
                    if (purpose != null && l.getPurpose() != purpose) return null;
                    if (typeId != null && !typeId.equals(property.getTypeId())) return null;
                    if (cityId != null && !cityId.equals(property.getCityId())) return null;
                    if (areaId != null && !areaId.equals(property.getAreaId())) return null;
                    if (cityQ != null && !containsIgnoreCase(property.getCity(), cityQ)) return null;
                    if (suburbQ != null && !containsIgnoreCase(property.getSuburb(), suburbQ)
                            && !containsIgnoreCase(property.getStreet(), suburbQ)
                            && !containsIgnoreCase(property.getCity(), suburbQ)) return null;
                    if (minPrice != null && (l.getPrice() == null || l.getPrice().compareTo(minPrice) < 0)) return null;
                    if (maxPrice != null && (l.getPrice() == null || l.getPrice().compareTo(maxPrice) > 0)) return null;
                    if (featuredOnly && (l.getFeaturedUntil() == null || !l.getFeaturedUntil().isAfter(now))) return null;
                    if (minBedrooms != null && (property.getBedrooms() == null || property.getBedrooms() < minBedrooms)) return null;
                    if (minSizeSqm != null && (property.getSizeSqm() == null || property.getSizeSqm().compareTo(minSizeSqm) < 0)) return null;
                    if (maxSizeSqm != null && (property.getSizeSqm() == null || property.getSizeSqm().compareTo(maxSizeSqm) > 0)) return null;
                    return map(l, property);
                })
                .filter(java.util.Objects::nonNull)
                .sorted(comparator(effectiveSort, now))
                .toList();
        return results;
    }

    private Comparator<ListingResponse> comparator(ListingSort sort, Instant now) {
        Comparator<ListingResponse> featuredFirst = Comparator.comparing((ListingResponse l) ->
                l.featuredUntil() != null && l.featuredUntil().isAfter(now) ? 0 : 1);
        Comparator<ListingResponse> newest = Comparator.comparing(ListingResponse::createdAt,
                Comparator.nullsLast(Comparator.reverseOrder()));
        Comparator<ListingResponse> priceAsc = Comparator.comparing(ListingResponse::price,
                Comparator.nullsLast(Comparator.naturalOrder()));
        Comparator<ListingResponse> areaAsc = Comparator.comparing(ListingResponse::sizeSqm,
                Comparator.nullsLast(Comparator.naturalOrder()));
        return switch (sort) {
            case PRICE_ASC -> priceAsc.thenComparing(newest);
            case PRICE_DESC -> priceAsc.reversed().thenComparing(newest);
            case AREA_ASC -> areaAsc.thenComparing(newest);
            case AREA_DESC -> areaAsc.reversed().thenComparing(newest);
            case NEWEST -> newest;
            case FEATURED -> featuredFirst.thenComparing(newest);
        };
    }

    private boolean containsIgnoreCase(String haystack, String needle) {
        return haystack != null && haystack.toLowerCase().contains(needle.toLowerCase());
    }

    @Transactional(readOnly = true)
    public SharePackResponse sharePack(UUID listingId) {
        ListingResponse listing = get(listingId);
        String location = joinLocation(listing.street(), listing.suburb(), listing.city());
        String purpose = listing.purpose() == ListingPurpose.RENT ? "To rent" : "For sale";
        String price = listing.currency() + " " + listing.price();
        String caption = purpose + " · " + (listing.typeLabel() != null ? listing.typeLabel() : "Property")
                + (location.isBlank() ? "" : " in " + location)
                + " · " + price
                + (listing.copy() != null ? "\n" + listing.copy() : "")
                + "\nListed on Cove";
        String encoded = URLEncoder.encode(caption, StandardCharsets.UTF_8);
        String marketplace = "https://www.facebook.com/marketplace/create/item";
        String pageShare = "https://www.facebook.com/sharer/sharer.php?u=" + URLEncoder.encode(appBaseUrl + "/marketplace?listing=" + listingId, StandardCharsets.UTF_8)
                + "&quote=" + encoded;
        return new SharePackResponse(
                caption,
                marketplace,
                pageShare,
                "Open Instagram and paste the caption into a new post or story. There is no reliable third-party posting API."
        );
    }

    public Listing requirePublished(UUID listingId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> ApiException.notFound(ErrorCode.LISTING_NOT_FOUND, "Listing not found"));
        if (!listing.isPublished() || listing.getStatus() != ListingStatus.PUBLISHED) {
            throw ApiException.badRequest(ErrorCode.LISTING_NOT_PUBLISHED, "This listing is not on the marketplace");
        }
        return listing;
    }

    public Listing require(UUID listingId) {
        return listingRepository.findById(listingId)
                .orElseThrow(() -> ApiException.notFound(ErrorCode.LISTING_NOT_FOUND, "Listing not found"));
    }

    private void applyFeatured(Listing listing, UUID actor, Integer featuredDays) {
        if (featuredDays == null || featuredDays <= 0) return;
        if (!subscriptionService.myAccess().unlockedFunctions().contains(SubscriptionFunction.MARKETPLACE_HIGHLIGHT)) {
            throw ApiException.forbidden(ErrorCode.MARKETPLACE_HIGHLIGHT_REQUIRED,
                    "A marketplace highlight plan is required to feature a listing");
        }
        listing.setFeaturedUntil(Instant.now().plus(featuredDays, ChronoUnit.DAYS));
    }

    // ─── Moderation (Cove staff with MODERATOR) ──────────────────

    @Transactional(readOnly = true)
    public List<ListingResponse> moderationQueue(ListingStatus status) {
        permissionService.requireStaff(true, StaffPermission.MODERATOR);
        List<Listing> listings = status == null
                ? listingRepository.findAll()
                : listingRepository.findByStatusOrderByCreatedAtDesc(status);
        Map<UUID, Property> properties = propertyService.getEntities(listings.stream().map(Listing::getPropertyId).distinct().toList());
        return listings.stream()
                .filter(l -> properties.containsKey(l.getPropertyId()))
                .sorted(Comparator.comparing(Listing::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(l -> map(l, properties.get(l.getPropertyId())))
                .toList();
    }

    public ListingResponse approve(UUID listingId) {
        UUID staff = permissionService.requireStaff(false, StaffPermission.MODERATOR);
        Listing listing = require(listingId);
        if (listing.getStatus() != ListingStatus.PENDING_REVIEW) {
            throw ApiException.conflict(ErrorCode.INVALID_PROPERTY_STATUS_TRANSITION, "Only listings waiting for review can be approved");
        }
        Property property = propertyService.getEntity(listing.getPropertyId());
        markReviewed(listing, staff, null);
        listing.setPublished(true);
        listing.setStatus(ListingStatus.PUBLISHED);
        propertyService.ensureListed(property, staff);
        auditLogService.record("LISTING", listingId, "APPROVED", ListingStatus.PENDING_REVIEW.name(), ListingStatus.PUBLISHED.name(), staff, null);
        eventPublisher.publishEvent(new ListingPublishedEvent(listing.getId(), property.getId(), listing.getPurpose(), listing.getListedByUserId()));
        return map(listing, property);
    }

    public ListingResponse reject(UUID listingId, String reason) {
        UUID staff = permissionService.requireStaff(false, StaffPermission.MODERATOR);
        Listing listing = require(listingId);
        if (listing.getStatus() != ListingStatus.PENDING_REVIEW) {
            throw ApiException.conflict(ErrorCode.INVALID_PROPERTY_STATUS_TRANSITION, "Only listings waiting for review can be rejected");
        }
        markReviewed(listing, staff, requireReason(reason));
        listing.setPublished(false);
        listing.setStatus(ListingStatus.REJECTED);
        auditLogService.record("LISTING", listingId, "REJECTED", ListingStatus.PENDING_REVIEW.name(), ListingStatus.REJECTED.name(), staff, reason);
        notifyLister(listing, "Listing not approved", "Your listing was not approved: " + reason.trim());
        eventPublisher.publishEvent(new bw.co.gimble.cove.events.ListingModeratedEvent(listing.getId(), ListingStatus.REJECTED, reason.trim()));
        return map(listing, propertyService.getEntity(listing.getPropertyId()));
    }

    public ListingResponse takeDown(UUID listingId, String reason) {
        UUID staff = permissionService.requireStaff(false, StaffPermission.MODERATOR);
        Listing listing = require(listingId);
        if (listing.getStatus() != ListingStatus.PUBLISHED) {
            throw ApiException.conflict(ErrorCode.INVALID_PROPERTY_STATUS_TRANSITION, "Only live listings can be taken down");
        }
        Property property = propertyService.getEntity(listing.getPropertyId());
        markReviewed(listing, staff, requireReason(reason));
        listing.setPublished(false);
        listing.setStatus(ListingStatus.TAKEN_DOWN);
        long remaining = listingRepository.countByPropertyIdAndPublishedTrueAndStatus(property.getId(), ListingStatus.PUBLISHED);
        propertyService.maybeUnlist(property, staff, remaining);
        auditLogService.record("LISTING", listingId, "TAKEN_DOWN", ListingStatus.PUBLISHED.name(), ListingStatus.TAKEN_DOWN.name(), staff, reason);
        notifyLister(listing, "Listing taken down", "A Cove moderator removed your listing: " + reason.trim());
        eventPublisher.publishEvent(new bw.co.gimble.cove.events.ListingModeratedEvent(listing.getId(), ListingStatus.TAKEN_DOWN, reason.trim()));
        return map(listing, property);
    }

    public ListingResponse restore(UUID listingId) {
        UUID staff = permissionService.requireStaff(false, StaffPermission.MODERATOR);
        Listing listing = require(listingId);
        if (listing.getStatus() != ListingStatus.TAKEN_DOWN) {
            throw ApiException.conflict(ErrorCode.INVALID_PROPERTY_STATUS_TRANSITION, "Only taken-down listings can be restored");
        }
        Property property = propertyService.getEntity(listing.getPropertyId());
        markReviewed(listing, staff, null);
        listing.setPublished(true);
        listing.setStatus(ListingStatus.PUBLISHED);
        propertyService.ensureListed(property, staff);
        auditLogService.record("LISTING", listingId, "RESTORED", ListingStatus.TAKEN_DOWN.name(), ListingStatus.PUBLISHED.name(), staff, null);
        return map(listing, property);
    }

    /** All listings for properties a company manages (company portal). */
    @Transactional(readOnly = true)
    public List<ListingResponse> forOrganisation(UUID organisationId) {
        Map<UUID, Property> properties = propertyService.byManagingOrganisation(organisationId);
        return listingRepository.findByPropertyIdIn(properties.keySet()).stream()
                .sorted(Comparator.comparing(Listing::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(l -> map(l, properties.get(l.getPropertyId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ListingResponse> mine() {
        UUID actor = AuthSupport.requireUserId();
        return listingRepository.findByListedByUserIdOrderByCreatedAtDesc(actor).stream()
                .map(l -> map(l, propertyService.getEntity(l.getPropertyId())))
                .toList();
    }

    public ListingResponse toResponse(Listing listing) {
        return map(listing, propertyService.getEntity(listing.getPropertyId()));
    }

    private boolean isVerifiedCompanyListing(Property property) {
        UUID orgId = property.getManagingOrganisationId();
        return orgId != null && organisationRepository.findById(orgId)
                .map(o -> o.getVerificationStatus() == bw.co.gimble.cove.organisations.OrgVerificationStatus.VERIFIED)
                .orElse(false);
    }

    private void markReviewed(Listing listing, UUID staff, String note) {
        listing.setReviewNote(note);
        listing.setReviewedByUserId(staff);
        listing.setReviewedAt(Instant.now());
    }

    private String requireReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "Give a reason the lister will see");
        }
        return reason.trim();
    }

    private void notifyLister(Listing listing, String title, String message) {
        notificationService.create(listing.getListedByUserId(), bw.co.gimble.cove.notifications.NotificationType.LISTING_REVIEWED,
                title, message, "LISTING", listing.getId());
    }

    private ListingResponse map(Listing l, Property p) {
        PropertyType type = typeRepository.findById(p.getTypeId()).orElse(null);
        var org = p.getManagingOrganisationId() == null ? null
                : organisationRepository.findById(p.getManagingOrganisationId()).orElse(null);
        List<PropertyResponse.MediaResponse> media = mediaRepository
                .findByPropertyIdOrderBySortOrderAscCreatedAtAsc(p.getId()).stream()
                .map(PropertyResponse.MediaResponse::of)
                .toList();
        return new ListingResponse(
                l.getId(), l.getPropertyId(), l.getPurpose(), l.getStatus(), l.isPublished(),
                l.getPrice(), l.getCurrency(), l.getRentPeriod(), l.getCopy(), l.getFeaturedUntil(),
                p.getStatus(), p.getCity(), p.getSuburb(), p.getStreet(),
                type != null ? type.getLabel() : null,
                p.getBedrooms(), p.getBathrooms(), p.getParkingSpaces(), p.getSizeSqm(), media, l.getCreatedAt(),
                p.getCityId(), p.getAreaId(), l.getReviewNote(),
                org != null ? org.getId() : null,
                org != null ? org.getName() : null,
                org != null && org.getVerificationStatus() == bw.co.gimble.cove.organisations.OrgVerificationStatus.VERIFIED
        );
    }

    private String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private String joinLocation(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.isBlank()) continue;
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(part);
        }
        return sb.toString();
    }
}
