package bw.co.gimble.cove.properties;

import bw.co.gimble.cove.locations.LocationService;
import bw.co.gimble.cove.common.ApiException;
import bw.co.gimble.cove.common.AuditLogService;
import bw.co.gimble.cove.common.ErrorCode;
import bw.co.gimble.cove.events.PropertyStatusChangedEvent;
import bw.co.gimble.cove.permission.PermissionService;
import bw.co.gimble.cove.properties.dto.CreatePropertyRequest;
import bw.co.gimble.cove.properties.dto.OwnerSplitRequest;
import bw.co.gimble.cove.properties.dto.PropertyResponse;
import bw.co.gimble.cove.properties.dto.UpdatePropertyRequest;
import bw.co.gimble.cove.role.RoleStatus;
import bw.co.gimble.cove.role.UserRoleRepository;
import bw.co.gimble.cove.security.AuthSupport;
import bw.co.gimble.cove.storage.StorageService;
import bw.co.gimble.cove.storage.dto.FileValidation;
import bw.co.gimble.cove.storage.dto.UploadedFile;
import bw.co.gimble.cove.subscriptions.SubscriptionFunction;
import bw.co.gimble.cove.subscriptions.SubscriptionService;
import bw.co.gimble.cove.user.User;
import bw.co.gimble.cove.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final PropertyOwnerRepository ownerRepository;
    private static final int MAX_MEDIA_PER_PROPERTY = 40;

    private final PropertyMediaRepository mediaRepository;
    private final LocationService locationService;
    private final PropertyTypeRepository typeRepository;
    private final PropertyTypeService propertyTypeService;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PermissionService permissionService;
    private final SubscriptionService subscriptionService;
    private final StorageService storageService;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher eventPublisher;

    public PropertyResponse create(CreatePropertyRequest request) {
        UUID actor = AuthSupport.requireUserId();
        requireNotStaff(actor);
        propertyTypeService.requireActive(request.typeId());

        UUID orgId = request.managingOrganisationId();
        if (orgId != null) {
            requireAgencyOnOrg(actor, orgId);
        }
        LocationService.Place place = locationService.resolve(request.cityId(), request.areaId());

        Property property = Property.builder()
                .typeId(request.typeId())
                .status(PropertyStatus.UNLISTED)
                .description(request.description())
                .bedrooms(request.bedrooms())
                .bathrooms(request.bathrooms())
                .parkingSpaces(request.parkingSpaces())
                .sizeSqm(request.sizeSqm())
                .country(blankToDefault(request.country(), "BW"))
                .city(place.city().getName())
                .suburb(place.area().getName())
                .cityId(place.city().getId())
                .areaId(place.area().getId())
                .street(request.street())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .managingOrganisationId(orgId)
                .build();
        property = propertyRepository.save(property);

        List<OwnerSplitRequest> owners = request.owners();
        if (owners == null || owners.isEmpty()) {
            owners = List.of(new OwnerSplitRequest(actor, BigDecimal.valueOf(100)));
        }
        replaceOwnersInternal(property.getId(), owners);

        auditLogService.record("PROPERTY", property.getId(), "CREATED", null, PropertyStatus.UNLISTED.name(), actor, null);
        return map(property);
    }

    public PropertyResponse update(UUID id, UpdatePropertyRequest request) {
        UUID actor = AuthSupport.requireUserId();
        Property property = requireViewable(id, actor);
        requireCanManage(property, actor);

        if (request.typeId() != null) {
            propertyTypeService.requireActive(request.typeId());
            property.setTypeId(request.typeId());
        }
        if (request.description() != null) property.setDescription(request.description());
        if (request.bedrooms() != null) property.setBedrooms(request.bedrooms());
        if (request.bathrooms() != null) property.setBathrooms(request.bathrooms());
        if (request.parkingSpaces() != null) property.setParkingSpaces(request.parkingSpaces());
        if (request.sizeSqm() != null) property.setSizeSqm(request.sizeSqm());
        if (request.country() != null) property.setCountry(request.country());
        if (request.cityId() != null || request.areaId() != null) {
            LocationService.Place place = locationService.resolve(request.cityId(), request.areaId());
            property.setCityId(place.city().getId());
            property.setAreaId(place.area().getId());
            property.setCity(place.city().getName());
            property.setSuburb(place.area().getName());
        }
        if (request.street() != null) property.setStreet(request.street());
        if (request.latitude() != null) property.setLatitude(request.latitude());
        if (request.longitude() != null) property.setLongitude(request.longitude());
        if (request.managingOrganisationId() != null) {
            requireAgencyOnOrg(actor, request.managingOrganisationId());
            property.setManagingOrganisationId(request.managingOrganisationId());
        }
        return map(property);
    }

    public PropertyResponse replaceOwners(UUID propertyId, List<OwnerSplitRequest> owners) {
        UUID actor = AuthSupport.requireUserId();
        Property property = requireViewable(propertyId, actor);
        requireCanManage(property, actor);
        replaceOwnersInternal(propertyId, owners);
        return map(property);
    }

    public PropertyResponse addMedia(UUID propertyId, MultipartFile file) {
        return addMedia(propertyId, file, null);
    }

    /** [sourceUrl] marks media imported by URL (integration API). */
    public PropertyResponse addMedia(UUID propertyId, MultipartFile file, String sourceUrl) {
        UUID actor = AuthSupport.requireUserId();
        Property property = requireViewable(propertyId, actor);
        requireCanManage(property, actor);
        boolean video = FileValidation.requirePropertyMedia(file);
        int order = (int) mediaRepository.countByPropertyId(propertyId);
        if (order >= MAX_MEDIA_PER_PROPERTY) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED,
                    "A property can have at most " + MAX_MEDIA_PER_PROPERTY + " photos and videos");
        }
        UploadedFile uploaded = storageService.uploadPropertyMedia(propertyId, file);
        mediaRepository.save(PropertyMedia.builder()
                .propertyId(propertyId)
                .storageKey(uploaded.key())
                .url(uploaded.publicUrl())
                .sortOrder(order)
                .mediaType(video ? "VIDEO" : "IMAGE")
                .sourceUrl(sourceUrl)
                .build());
        return map(property);
    }

    /** Rewrites sort order to match the given ids; the first one becomes the cover. Unknown ids are rejected. */
    public PropertyResponse reorderMedia(UUID propertyId, List<UUID> orderedIds) {
        UUID actor = AuthSupport.requireUserId();
        Property property = requireViewable(propertyId, actor);
        requireCanManage(property, actor);
        List<PropertyMedia> current = mediaRepository.findByPropertyIdOrderBySortOrderAscCreatedAtAsc(propertyId);
        Map<UUID, PropertyMedia> byId = current.stream().collect(Collectors.toMap(PropertyMedia::getId, m -> m));
        if (orderedIds == null || !byId.keySet().containsAll(orderedIds)) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "Media order contains unknown items");
        }
        int i = 0;
        for (UUID id : orderedIds) {
            byId.remove(id).setSortOrder(i++);
        }
        // Anything not mentioned keeps its relative order after the listed ones.
        for (PropertyMedia rest : current) {
            if (byId.containsKey(rest.getId())) rest.setSortOrder(i++);
        }
        return map(property);
    }

    public void deleteMedia(UUID propertyId, UUID mediaId) {
        UUID actor = AuthSupport.requireUserId();
        Property property = requireViewable(propertyId, actor);
        requireCanManage(property, actor);
        PropertyMedia media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> ApiException.notFound("Media not found"));
        if (!media.getPropertyId().equals(propertyId)) {
            throw ApiException.notFound("Media not found");
        }
        storageService.deletePropertyMedia(media.getStorageKey());
        mediaRepository.delete(media);
    }

    @Transactional(readOnly = true)
    public PropertyResponse get(UUID id) {
        UUID actor = AuthSupport.requireUserId();
        return map(requireViewable(id, actor));
    }

    @Transactional(readOnly = true)
    public List<PropertyResponse> mine() {
        UUID actor = AuthSupport.requireUserId();
        Set<UUID> seen = new HashSet<>();
        List<Property> all = new ArrayList<>();
        for (Property p : propertyRepository.findOwnedBy(actor)) {
            if (seen.add(p.getId())) all.add(p);
        }
        List<UUID> orgIds = userRoleRepository.findDistinctOrganisationIdsByUserIdAndStatus(actor, RoleStatus.ACTIVE);
        if (!orgIds.isEmpty()) {
            for (Property p : propertyRepository.findByManagingOrganisationIdInOrderByCreatedAtDesc(orgIds)) {
                if (seen.add(p.getId())) all.add(p);
            }
        }
        if (permissionService.isPlatformAdmin(actor)) {
            for (Property p : propertyRepository.findAll()) {
                if (seen.add(p.getId())) all.add(p);
            }
        }
        return all.stream().map(this::map).toList();
    }

    public void transition(Property property, PropertyStatus to, UUID actor, String note) {
        PropertyStatus from = property.getStatus();
        if (from == to) return;
        if (!isAllowed(from, to)) {
            throw ApiException.badRequest(ErrorCode.INVALID_PROPERTY_STATUS_TRANSITION,
                    "Cannot move a property from " + from + " to " + to);
        }
        property.setStatus(to);
        auditLogService.record("PROPERTY", property.getId(), "STATUS", from.name(), to.name(), actor, note);
        eventPublisher.publishEvent(new PropertyStatusChangedEvent(property.getId(), from.name(), to.name(), actor));
    }

    public void ensureListed(Property property, UUID actor) {
        if (property.getStatus() == PropertyStatus.UNLISTED) {
            transition(property, PropertyStatus.LISTED, actor, "listing published");
        }
    }

    public void maybeUnlist(Property property, UUID actor, long remainingPublished) {
        if (remainingPublished == 0 && property.getStatus() == PropertyStatus.LISTED) {
            transition(property, PropertyStatus.UNLISTED, actor, "last listing unpublished");
        }
    }

    public void requirePublishReady(Property property) {
        List<PropertyOwner> owners = ownerRepository.findByPropertyId(property.getId());
        if (owners.isEmpty()) {
            throw ApiException.badRequest(ErrorCode.OWNERSHIP_SPLITS_INVALID, "Add at least one owner before publishing");
        }
        BigDecimal sum = owners.stream().map(PropertyOwner::getSplitPercent).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sum.compareTo(new BigDecimal("100.00")) != 0) {
            throw ApiException.badRequest(ErrorCode.OWNERSHIP_SPLITS_INVALID, "Owner split percentages must add up to 100 before publishing");
        }
        if (mediaRepository.countPhotos(property.getId()) < 1) {
            throw ApiException.badRequest(ErrorCode.MEDIA_REQUIRED, "Add at least one photo before publishing");
        }
    }

    public Property getEntity(UUID id) {
        return propertyRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(ErrorCode.PROPERTY_NOT_FOUND, "Property not found"));
    }

    @Transactional(readOnly = true)
    public Map<UUID, Property> getEntities(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return propertyRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Property::getId, p -> p));
    }

    public Property requireViewable(UUID id, UUID actor) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(ErrorCode.PROPERTY_NOT_FOUND, "Property not found"));
        if (!canView(property, actor)) {
            throw ApiException.forbidden(ErrorCode.ORG_ROLE_FORBIDDEN, "You cannot access this property");
        }
        return property;
    }

    public Property requireManageable(UUID id, UUID actor) {
        Property property = requireViewable(id, actor);
        requireCanManage(property, actor);
        return property;
    }

    public boolean canView(Property property, UUID actor) {
        if (permissionService.isPlatformAdmin(actor)) return true;
        if (ownerRepository.existsByPropertyIdAndUserId(property.getId(), actor)) return true;
        UUID orgId = property.getManagingOrganisationId();
        return orgId != null && permissionService.isOrgMember(actor, orgId);
    }

    public boolean isOwner(UUID propertyId, UUID userId) {
        return ownerRepository.existsByPropertyIdAndUserId(propertyId, userId);
    }

    public List<PropertyOwner> ownersOf(UUID propertyId) {
        return ownerRepository.findByPropertyId(propertyId);
    }

    public void requireCanManage(Property property, UUID actor) {
        // Staff deliberately can't edit listings' properties; they moderate via ListingService.
        if (ownerRepository.existsByPropertyIdAndUserId(property.getId(), actor)) return;
        UUID orgId = property.getManagingOrganisationId();
        if (orgId != null && permissionService.isAgencyStaff(actor, orgId)) return;
        throw ApiException.forbidden(ErrorCode.ORG_ROLE_FORBIDDEN, "You cannot change this property");
    }

    public void requireAgencyOnOrg(UUID actor, UUID orgId) {
        if (!permissionService.isAgencyStaff(actor, orgId)) {
            throw ApiException.forbidden(ErrorCode.ORG_ROLE_FORBIDDEN, "Only an agent of this organisation can assign it to a property");
        }
        if (!subscriptionService.myAccess().unlockedFunctions().contains(SubscriptionFunction.AGENCY)) {
            throw ApiException.forbidden(ErrorCode.AGENCY_SUBSCRIPTION_REQUIRED,
                    "An Agency subscription is required to list on behalf of an organisation");
        }
    }

    private void replaceOwnersInternal(UUID propertyId, List<OwnerSplitRequest> owners) {
        if (owners == null || owners.isEmpty()) {
            throw ApiException.badRequest(ErrorCode.OWNERSHIP_SPLITS_INVALID, "At least one owner is required");
        }
        ownerRepository.deleteByPropertyId(propertyId);
        ownerRepository.flush();
        for (OwnerSplitRequest split : owners) {
            if (split.userId() == null || split.splitPercent() == null) {
                throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "Each owner needs a userId and splitPercent");
            }
            if (!userRepository.existsById(split.userId())) {
                throw ApiException.notFound("Owner user not found: " + split.userId());
            }
            ownerRepository.save(PropertyOwner.builder()
                    .propertyId(propertyId)
                    .userId(split.userId())
                    .splitPercent(split.splitPercent().setScale(2, RoundingMode.HALF_UP))
                    .build());
        }
    }

    private boolean isAllowed(PropertyStatus from, PropertyStatus to) {
        return switch (from) {
            case UNLISTED -> to == PropertyStatus.LISTED;
            case LISTED -> to == PropertyStatus.UNLISTED
                    || to == PropertyStatus.LEASED
                    || to == PropertyStatus.SOLD
                    || to == PropertyStatus.UNDER_MANAGEMENT;
            case LEASED -> to == PropertyStatus.UNDER_MANAGEMENT;
            case SOLD, UNDER_MANAGEMENT -> false;
        };
    }

    @Transactional(readOnly = true)
    public Map<UUID, Property> byManagingOrganisation(UUID organisationId) {
        return propertyRepository.findByManagingOrganisationId(organisationId).stream()
                .collect(Collectors.toMap(Property::getId, p -> p));
    }

    /** Cove staff monitor and approve; they never list properties themselves. */
    public void requireNotStaff(UUID actor) {
        if (permissionService.isPlatformAdmin(actor)) {
            throw ApiException.forbidden(ErrorCode.PLATFORM_ADMIN_ONLY,
                    "Cove staff accounts can't list properties. Use a separate personal or company account.");
        }
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    public PropertyResponse map(Property p) {
        PropertyType type = typeRepository.findById(p.getTypeId()).orElse(null);
        List<PropertyResponse.OwnerResponse> owners = ownerRepository.findByPropertyId(p.getId()).stream()
                .map(o -> new PropertyResponse.OwnerResponse(
                        o.getUserId(),
                        userRepository.findById(o.getUserId()).map(User::getEmail).orElse(null),
                        o.getSplitPercent()))
                .collect(Collectors.toList());
        List<PropertyResponse.MediaResponse> media = mediaRepository
                .findByPropertyIdOrderBySortOrderAscCreatedAtAsc(p.getId()).stream()
                .map(PropertyResponse.MediaResponse::of)
                .toList();
        return new PropertyResponse(
                p.getId(), p.getTypeId(),
                type != null ? type.getCode() : null,
                type != null ? type.getLabel() : null,
                p.getStatus(), p.getDescription(),
                p.getBedrooms(), p.getBathrooms(), p.getParkingSpaces(), p.getSizeSqm(),
                p.getCountry(), p.getCity(), p.getSuburb(), p.getStreet(),
                p.getCityId(), p.getAreaId(), p.getExternalRef(),
                p.getLatitude(), p.getLongitude(), p.getManagingOrganisationId(),
                owners, media, p.getCreatedAt(), p.getVersion()
        );
    }
}
