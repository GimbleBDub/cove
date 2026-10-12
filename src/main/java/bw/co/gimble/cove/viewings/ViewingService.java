package bw.co.gimble.cove.viewings;

import bw.co.gimble.cove.common.ApiException;
import bw.co.gimble.cove.common.AuditLogService;
import bw.co.gimble.cove.common.ErrorCode;
import bw.co.gimble.cove.events.ViewingBookedEvent;
import bw.co.gimble.cove.leads.LeadService;
import bw.co.gimble.cove.listings.Listing;
import bw.co.gimble.cove.listings.ListingService;
import bw.co.gimble.cove.properties.Property;
import bw.co.gimble.cove.properties.PropertyService;
import bw.co.gimble.cove.security.AuthSupport;
import bw.co.gimble.cove.viewings.dto.CreateViewingRequest;
import bw.co.gimble.cove.viewings.dto.UpdateViewingStatusRequest;
import bw.co.gimble.cove.viewings.dto.ViewingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ViewingService {

    private final ViewingRepository viewingRepository;
    private final ListingService listingService;
    private final PropertyService propertyService;
    private final LeadService leadService;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher eventPublisher;

    public ViewingResponse request(CreateViewingRequest request) {
        UUID actor = AuthSupport.requireUserId();
        Listing listing = listingService.requirePublished(request.listingId());
        if (!request.endsAt().isAfter(request.startsAt())) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "Viewing end must be after start");
        }
        if (request.leadId() != null) {
            leadService.requireParticipant(request.leadId(), actor);
        }
        Viewing viewing = viewingRepository.save(Viewing.builder()
                .listingId(listing.getId())
                .leadId(request.leadId())
                .startsAt(request.startsAt())
                .endsAt(request.endsAt())
                .status(ViewingStatus.REQUESTED)
                .requestedByUserId(actor)
                .build());
        if (request.leadId() != null) {
            leadService.markViewing(request.leadId(), actor);
        }
        auditLogService.record("VIEWING", viewing.getId(), "REQUESTED", null, ViewingStatus.REQUESTED.name(), actor, null);
        eventPublisher.publishEvent(new ViewingBookedEvent(viewing.getId(), listing.getId(), actor));
        return map(viewing);
    }

    public ViewingResponse updateStatus(UUID id, UpdateViewingStatusRequest request) {
        UUID actor = AuthSupport.requireUserId();
        Viewing viewing = viewingRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(ErrorCode.VIEWING_NOT_FOUND, "Viewing not found"));
        Listing listing = listingService.require(viewing.getListingId());
        Property property = propertyService.getEntity(listing.getPropertyId());
        boolean requester = viewing.getRequestedByUserId().equals(actor);
        if (request.status() == ViewingStatus.CANCELLED) {
            if (!requester) propertyService.requireCanManage(property, actor);
        } else {
            propertyService.requireCanManage(property, actor);
            if (request.status() == ViewingStatus.CONFIRMED) {
                viewing.setConfirmedByUserId(actor);
            }
        }
        ViewingStatus from = viewing.getStatus();
        viewing.setStatus(request.status());
        auditLogService.record("VIEWING", viewing.getId(), "STATUS", from.name(), request.status().name(), actor, null);
        return map(viewing);
    }

    @Transactional(readOnly = true)
    public List<ViewingResponse> mine() {
        UUID actor = AuthSupport.requireUserId();
        return viewingRepository.findAll().stream()
                .filter(v -> {
                    if (v.getRequestedByUserId().equals(actor)) return true;
                    Listing listing = listingService.require(v.getListingId());
                    Property property = propertyService.getEntity(listing.getPropertyId());
                    return propertyService.canView(property, actor);
                })
                .sorted(Comparator.comparing(Viewing::getStartsAt).reversed())
                .map(this::map)
                .toList();
    }

    private ViewingResponse map(Viewing v) {
        return new ViewingResponse(
                v.getId(), v.getListingId(), v.getLeadId(), v.getStartsAt(), v.getEndsAt(),
                v.getStatus(), v.getRequestedByUserId(), v.getConfirmedByUserId(), v.getCreatedAt()
        );
    }
}
