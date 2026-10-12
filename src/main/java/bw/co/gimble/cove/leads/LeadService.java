package bw.co.gimble.cove.leads;

import bw.co.gimble.cove.common.ApiException;
import bw.co.gimble.cove.common.AuditLogService;
import bw.co.gimble.cove.common.ErrorCode;
import bw.co.gimble.cove.events.LeadCreatedEvent;
import bw.co.gimble.cove.leads.dto.*;
import bw.co.gimble.cove.listings.Listing;
import bw.co.gimble.cove.listings.ListingService;
import bw.co.gimble.cove.properties.Property;
import bw.co.gimble.cove.properties.PropertyService;
import bw.co.gimble.cove.security.AuthSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class LeadService {

    private final LeadRepository leadRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ListingService listingService;
    private final PropertyService propertyService;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher eventPublisher;

    public LeadResponse enquire(CreateLeadRequest request) {
        UUID actor = AuthSupport.requireUserId();
        Listing listing = listingService.requirePublished(request.listingId());
        Property property = propertyService.getEntity(listing.getPropertyId());
        Lead lead = leadRepository.findByListingIdAndInquirerUserId(listing.getId(), actor)
                .orElseGet(() -> {
                    Lead created = leadRepository.save(Lead.builder()
                            .listingId(listing.getId())
                            .inquirerUserId(actor)
                            .assignedAgentUserId(listing.getListedByUserId())
                            .stage(LeadStage.NEW)
                            .build());
                    auditLogService.record("LEAD", created.getId(), "CREATED", null, LeadStage.NEW.name(), actor, null);
                    eventPublisher.publishEvent(new LeadCreatedEvent(
                            created.getId(), listing.getId(), actor, listing.getListedByUserId()));
                    return created;
                });
        if (request.message() != null && !request.message().isBlank()) {
            chatMessageRepository.save(ChatMessage.builder()
                    .leadId(lead.getId())
                    .senderUserId(actor)
                    .body(request.message().trim())
                    .build());
        }
        return map(lead);
    }

    public LeadResponse updateStage(UUID leadId, UpdateLeadStageRequest request) {
        UUID actor = AuthSupport.requireUserId();
        Lead lead = requireManageable(leadId, actor);
        LeadStage from = lead.getStage();
        lead.setStage(request.stage());
        auditLogService.record("LEAD", lead.getId(), "STAGE", from.name(), request.stage().name(), actor, null);
        return map(lead);
    }

    public void markViewing(UUID leadId, UUID actor) {
        Lead lead = requireParticipant(leadId, actor);
        if (lead.getStage() == LeadStage.NEW || lead.getStage() == LeadStage.CONTACTED) {
            LeadStage from = lead.getStage();
            lead.setStage(LeadStage.VIEWING);
            auditLogService.record("LEAD", lead.getId(), "STAGE", from.name(), LeadStage.VIEWING.name(), actor, "viewing requested");
        }
    }

    public ChatMessageResponse sendMessage(UUID leadId, SendMessageRequest request) {
        UUID actor = AuthSupport.requireUserId();
        Lead lead = requireParticipant(leadId, actor);
        ChatMessage msg = chatMessageRepository.save(ChatMessage.builder()
                .leadId(lead.getId())
                .senderUserId(actor)
                .body(request.body().trim())
                .build());
        return map(msg);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> messages(UUID leadId) {
        UUID actor = AuthSupport.requireUserId();
        requireParticipant(leadId, actor);
        return chatMessageRepository.findByLeadIdOrderByCreatedAtAsc(leadId).stream().map(this::map).toList();
    }

    @Transactional(readOnly = true)
    public List<LeadResponse> mine() {
        UUID actor = AuthSupport.requireUserId();
        Map<UUID, Lead> byId = new LinkedHashMap<>();
        leadRepository.findByInquirerUserIdOrderByCreatedAtDesc(actor).forEach(l -> byId.put(l.getId(), l));
        leadRepository.findByAssignedAgentUserIdOrderByCreatedAtDesc(actor).forEach(l -> byId.put(l.getId(), l));
        List<Lead> extra = new ArrayList<>();
        for (Lead lead : leadRepository.findAll()) {
            if (byId.containsKey(lead.getId())) continue;
            Listing listing = listingService.require(lead.getListingId());
            Property property = propertyService.getEntity(listing.getPropertyId());
            if (propertyService.canView(property, actor)) {
                extra.add(lead);
            }
        }
        extra.forEach(l -> byId.put(l.getId(), l));
        return byId.values().stream()
                .sorted(Comparator.comparing(Lead::getCreatedAt).reversed())
                .map(this::map)
                .toList();
    }

    public Lead requireParticipant(UUID leadId, UUID actor) {
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> ApiException.notFound(ErrorCode.LEAD_NOT_FOUND, "Lead not found"));
        if (lead.getInquirerUserId().equals(actor) || (lead.getAssignedAgentUserId() != null && lead.getAssignedAgentUserId().equals(actor))) {
            return lead;
        }
        Listing listing = listingService.require(lead.getListingId());
        Property property = propertyService.getEntity(listing.getPropertyId());
        if (propertyService.canView(property, actor)) return lead;
        throw ApiException.forbidden(ErrorCode.ORG_ROLE_FORBIDDEN, "You cannot access this lead");
    }

    private Lead requireManageable(UUID leadId, UUID actor) {
        Lead lead = requireParticipant(leadId, actor);
        if (lead.getInquirerUserId().equals(actor) && (lead.getAssignedAgentUserId() == null || !lead.getAssignedAgentUserId().equals(actor))) {
            Listing listing = listingService.require(lead.getListingId());
            Property property = propertyService.getEntity(listing.getPropertyId());
            propertyService.requireCanManage(property, actor);
        }
        return lead;
    }

    private LeadResponse map(Lead l) {
        return new LeadResponse(l.getId(), l.getListingId(), l.getInquirerUserId(), l.getAssignedAgentUserId(), l.getStage(), l.getCreatedAt());
    }

    private ChatMessageResponse map(ChatMessage m) {
        return new ChatMessageResponse(m.getId(), m.getLeadId(), m.getSenderUserId(), m.getBody(), m.getCreatedAt());
    }
}
