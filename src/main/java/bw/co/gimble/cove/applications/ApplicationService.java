package bw.co.gimble.cove.applications;

import bw.co.gimble.cove.applications.dto.ApplicationResponse;
import bw.co.gimble.cove.applications.dto.CreateApplicationRequest;
import bw.co.gimble.cove.applications.dto.LeaseResponse;
import bw.co.gimble.cove.applications.dto.ReviewApplicationRequest;
import bw.co.gimble.cove.common.ApiException;
import bw.co.gimble.cove.common.AuditLogService;
import bw.co.gimble.cove.common.ErrorCode;
import bw.co.gimble.cove.events.ApplicationSubmittedEvent;
import bw.co.gimble.cove.events.LeaseSignedEvent;
import bw.co.gimble.cove.listings.Listing;
import bw.co.gimble.cove.listings.ListingPurpose;
import bw.co.gimble.cove.listings.ListingService;
import bw.co.gimble.cove.profile.models.UserKyc;
import bw.co.gimble.cove.profile.repositories.UserKycRepository;
import bw.co.gimble.cove.properties.Property;
import bw.co.gimble.cove.properties.PropertyOwner;
import bw.co.gimble.cove.properties.PropertyService;
import bw.co.gimble.cove.properties.PropertyStatus;
import bw.co.gimble.cove.security.AuthSupport;
import bw.co.gimble.cove.storage.StorageService;
import bw.co.gimble.cove.storage.dto.UploadedFile;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ApplicationService {

    private final ListingApplicationRepository applicationRepository;
    private final SignatureEventRepository signatureEventRepository;
    private final LeaseRepository leaseRepository;
    private final ListingService listingService;
    private final PropertyService propertyService;
    private final UserKycRepository userKycRepository;
    private final StorageService storageService;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher eventPublisher;

    public ApplicationResponse submit(CreateApplicationRequest request) {
        UUID actor = AuthSupport.requireUserId();
        Listing listing = listingService.requirePublished(request.listingId());
        if (request.type() == ApplicationType.RENTAL && listing.getPurpose() != ListingPurpose.RENT) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "Rental applications are only for rent listings");
        }
        if (request.type() == ApplicationType.PURCHASE_OFFER && listing.getPurpose() != ListingPurpose.SALE) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "Purchase offers are only for sale listings");
        }
        ListingApplication app = applicationRepository.save(ListingApplication.builder()
                .listingId(listing.getId())
                .applicantUserId(actor)
                .type(request.type())
                .status(ApplicationStatus.SUBMITTED)
                .occupants(request.occupants())
                .monthlyIncome(request.monthlyIncome())
                .offerAmount(request.offerAmount())
                .conditions(request.conditions())
                .referralSource(request.referralSource())
                .proposedStartDate(request.proposedStartDate())
                .proposedEndDate(request.proposedEndDate())
                .proposedRent(request.proposedRent() != null ? request.proposedRent() : listing.getPrice())
                .proposedDeposit(request.proposedDeposit())
                .build());
        auditLogService.record("APPLICATION", app.getId(), "SUBMITTED", null, ApplicationStatus.SUBMITTED.name(), actor, request.type().name());
        eventPublisher.publishEvent(new ApplicationSubmittedEvent(app.getId(), listing.getId(), actor));
        return map(app, true);
    }

    public ApplicationResponse review(UUID id, ReviewApplicationRequest request) {
        UUID actor = AuthSupport.requireUserId();
        ListingApplication app = require(id);
        Listing listing = listingService.require(app.getListingId());
        Property property = propertyService.getEntity(listing.getPropertyId());
        propertyService.requireCanManage(property, actor);
        if (request.status() != ApplicationStatus.UNDER_REVIEW
                && request.status() != ApplicationStatus.ACCEPTED
                && request.status() != ApplicationStatus.REJECTED) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "Review can only set UNDER_REVIEW, ACCEPTED or REJECTED");
        }
        ApplicationStatus from = app.getStatus();
        app.setStatus(request.status());
        auditLogService.record("APPLICATION", app.getId(), "REVIEW", from.name(), request.status().name(), actor, null);
        return map(app, true);
    }

    public ApplicationResponse withdraw(UUID id) {
        UUID actor = AuthSupport.requireUserId();
        ListingApplication app = require(id);
        if (!app.getApplicantUserId().equals(actor)) {
            throw ApiException.forbidden(ErrorCode.ORG_ROLE_FORBIDDEN, "Only the applicant can withdraw");
        }
        ApplicationStatus from = app.getStatus();
        app.setStatus(ApplicationStatus.WITHDRAWN);
        auditLogService.record("APPLICATION", app.getId(), "WITHDRAWN", from.name(), ApplicationStatus.WITHDRAWN.name(), actor, null);
        return map(app, false);
    }

    public ApplicationResponse sign(UUID id, String typedName, MultipartFile file) {
        UUID actor = AuthSupport.requireUserId();
        ListingApplication app = require(id);
        if (app.getStatus() != ApplicationStatus.ACCEPTED) {
            throw ApiException.badRequest(ErrorCode.APPLICATION_NOT_ACCEPTED, "The application must be accepted before signing");
        }
        Listing listing = listingService.require(app.getListingId());
        Property property = propertyService.getEntity(listing.getPropertyId());
        boolean applicant = app.getApplicantUserId().equals(actor);
        boolean owner = propertyService.isOwner(property.getId(), actor);
        if (!applicant && !owner) {
            propertyService.requireCanManage(property, actor);
            owner = true;
        }
        String party = applicant ? "APPLICANT" : "OWNER";
        if (signatureEventRepository.existsByApplicationIdAndSignerUserIdAndParty(app.getId(), actor, party)) {
            throw ApiException.conflict(ErrorCode.SIGNATURE_ALREADY_RECORDED, "You have already signed");
        }
        if ((typedName == null || typedName.isBlank()) && (file == null || file.isEmpty())) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "Provide a typed name or a signature image");
        }

        String imageKey = null;
        String imageUrl = null;
        if (file != null && !file.isEmpty()) {
            UploadedFile uploaded = storageService.uploadSignature(app.getId(), file);
            imageKey = uploaded.key();
            imageUrl = uploaded.publicUrl();
        }
        String material = (typedName == null ? "" : typedName) + "|" + actor + "|" + app.getId() + "|" + Instantish.now();
        SignatureEvent event = signatureEventRepository.save(SignatureEvent.builder()
                .applicationId(app.getId())
                .signerUserId(actor)
                .party(party)
                .typedName(typedName)
                .imageKey(imageKey)
                .imageUrl(imageUrl)
                .sha256(sha256(material))
                .build());
        auditLogService.record("APPLICATION", app.getId(), "SIGNED", null, party, actor, event.getSha256());

        if (allRequiredSigned(app, property)) {
            complete(app, listing, property, actor);
        }
        return map(app, true);
    }

    @Transactional(readOnly = true)
    public ApplicationResponse get(UUID id) {
        UUID actor = AuthSupport.requireUserId();
        ListingApplication app = require(id);
        Listing listing = listingService.require(app.getListingId());
        Property property = propertyService.getEntity(listing.getPropertyId());
        boolean applicant = app.getApplicantUserId().equals(actor);
        boolean manager = propertyService.canView(property, actor);
        if (!applicant && !manager) {
            throw ApiException.forbidden(ErrorCode.ORG_ROLE_FORBIDDEN, "You cannot view this application");
        }
        return map(app, manager);
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> mine() {
        UUID actor = AuthSupport.requireUserId();
        return applicationRepository.findAll().stream()
                .filter(app -> {
                    if (app.getApplicantUserId().equals(actor)) return true;
                    Listing listing = listingService.require(app.getListingId());
                    Property property = propertyService.getEntity(listing.getPropertyId());
                    return propertyService.canView(property, actor);
                })
                .map(app -> {
                    Listing listing = listingService.require(app.getListingId());
                    Property property = propertyService.getEntity(listing.getPropertyId());
                    return map(app, propertyService.canView(property, actor));
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public LeaseResponse getLease(UUID applicationId) {
        UUID actor = AuthSupport.requireUserId();
        get(applicationId);
        Lease lease = leaseRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> ApiException.notFound("Lease not found"));
        return map(lease);
    }

    private void complete(ListingApplication app, Listing listing, Property property, UUID actor) {
        listingService.close(listing, actor, "application signed");
        if (app.getType() == ApplicationType.RENTAL) {
            Lease lease = leaseRepository.save(Lease.builder()
                    .propertyId(property.getId())
                    .listingId(listing.getId())
                    .applicationId(app.getId())
                    .tenantUserId(app.getApplicantUserId())
                    .startDate(app.getProposedStartDate())
                    .endDate(app.getProposedEndDate())
                    .rentAmount(app.getProposedRent() != null ? app.getProposedRent() : listing.getPrice())
                    .currency(listing.getCurrency())
                    .depositAmount(app.getProposedDeposit())
                    .build());
            propertyService.transition(property, PropertyStatus.UNDER_MANAGEMENT, actor, "lease e-signed");
            eventPublisher.publishEvent(new LeaseSignedEvent(lease.getId(), property.getId(), app.getId(), app.getApplicantUserId()));
        } else {
            propertyService.transition(property, PropertyStatus.SOLD, actor, "purchase offer e-signed");
        }
    }

    private boolean allRequiredSigned(ListingApplication app, Property property) {
        List<SignatureEvent> events = signatureEventRepository.findByApplicationId(app.getId());
        boolean applicantSigned = events.stream().anyMatch(e -> "APPLICANT".equals(e.getParty()));
        Set<UUID> ownerIds = propertyService.ownersOf(property.getId()).stream()
                .map(PropertyOwner::getUserId)
                .collect(Collectors.toSet());
        Set<UUID> signedOwners = events.stream()
                .filter(e -> "OWNER".equals(e.getParty()))
                .map(SignatureEvent::getSignerUserId)
                .collect(Collectors.toSet());
        boolean ownersSigned = !ownerIds.isEmpty() && signedOwners.containsAll(ownerIds);
        // Agent-managed properties: one OWNER-party signature from a manager counts if they are not an owner,
        // but owners still required when present. If there are owners, all must sign.
        return applicantSigned && ownersSigned;
    }

    private ListingApplication require(UUID id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(ErrorCode.APPLICATION_NOT_FOUND, "Application not found"));
    }

    private ApplicationResponse map(ListingApplication app, boolean includeKyc) {
        List<ApplicationResponse.ApplicantKyc> kyc = List.of();
        if (includeKyc) {
            kyc = userKycRepository.findByUserId(app.getApplicantUserId()).stream()
                    .map(this::mapKyc)
                    .toList();
        }
        List<ApplicationResponse.SignatureResponse> signatures = signatureEventRepository.findByApplicationId(app.getId())
                .stream()
                .map(s -> new ApplicationResponse.SignatureResponse(
                        s.getId(), s.getSignerUserId(), s.getParty(), s.getTypedName(), s.getCreatedAt()))
                .toList();
        return new ApplicationResponse(
                app.getId(), app.getListingId(), app.getApplicantUserId(), app.getType(), app.getStatus(),
                app.getOccupants(), app.getMonthlyIncome(), app.getOfferAmount(), app.getConditions(),
                app.getReferralSource(), app.getProposedStartDate(), app.getProposedEndDate(),
                app.getProposedRent(), app.getProposedDeposit(), kyc, signatures, app.getCreatedAt()
        );
    }

    private ApplicationResponse.ApplicantKyc mapKyc(UserKyc k) {
        return new ApplicationResponse.ApplicantKyc(k.getDocumentType(), k.getStatus(), k.getDocumentNumber());
    }

    private LeaseResponse map(Lease lease) {
        return new LeaseResponse(
                lease.getId(), lease.getPropertyId(), lease.getListingId(), lease.getApplicationId(),
                lease.getTenantUserId(), lease.getStartDate(), lease.getEndDate(),
                lease.getRentAmount(), lease.getCurrency(), lease.getDepositAmount()
        );
    }

    private String sha256(String material) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(material.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            return Integer.toHexString(material.hashCode());
        }
    }

    private static final class Instantish {
        static java.time.Instant now() {
            return java.time.Instant.now();
        }
    }
}
