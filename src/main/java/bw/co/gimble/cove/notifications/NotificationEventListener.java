package bw.co.gimble.cove.notifications;

import bw.co.gimble.cove.events.*;
import bw.co.gimble.cove.role.Role;
import bw.co.gimble.cove.role.RoleStatus;
import bw.co.gimble.cove.role.UserRole;
import bw.co.gimble.cove.role.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final UserRoleRepository userRoleRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserRegistered(UserRegisteredEvent event) {
        notificationService.create(
                event.userId(),
                NotificationType.WELCOME,
                "Welcome to Cove",
                "Your account is set up. Complete your profile to get started.",
                "USER",
                event.userId()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrganisationCreated(OrganisationCreatedEvent event) {
        notificationService.create(
                event.createdByUserId(),
                NotificationType.ORGANISATION_CREATED,
                "Organisation created",
                "\"" + event.organisationName() + "\" has been created and you are its admin.",
                "ORGANISATION",
                event.organisationId()
        );
    }

    /** Notify every active admin of the target org that someone wants to join. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrganisationJoinRequested(OrganisationJoinRequestedEvent event) {
        for (UserRole admin : userRoleRepository.findByOrganisationIdAndRoleAndStatus(
                event.organisationId(), Role.AGENCY_ADMIN, RoleStatus.ACTIVE)) {
            notificationService.create(
                    admin.getUserId(),
                    NotificationType.ORGANISATION_JOIN_REQUESTED,
                    "New join request",
                    "Someone has requested to join \"" + event.organisationName() + "\".",
                    "ORGANISATION_JOIN_REQUEST",
                    event.joinRequestId()
            );
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrganisationJoinReviewed(OrganisationJoinReviewedEvent event) {
        NotificationType type = event.approved()
                ? NotificationType.ORGANISATION_JOIN_APPROVED
                : NotificationType.ORGANISATION_JOIN_REJECTED;
        String title = event.approved() ? "Join request approved" : "Join request rejected";
        String message = event.approved()
                ? "Your request to join \"" + event.organisationName() + "\" was approved."
                : "Your request to join \"" + event.organisationName() + "\" was rejected"
                + (event.reason() != null ? ": " + event.reason() : ".");

        notificationService.create(
                event.requesterUserId(), type, title, message, "ORGANISATION", event.organisationId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRoleAssigned(RoleAssignedEvent event) {
        String scope = event.organisationId() != null ? " at " + event.organisationName() : "";
        notificationService.create(
                event.userId(),
                NotificationType.ROLE_ASSIGNED,
                "New role granted",
                "You have been granted the " + event.role().name() + " role" + scope + ".",
                "ROLE",
                event.userRoleId()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRoleRevoked(RoleRevokedEvent event) {
        String scope = event.organisationId() != null ? " at " + event.organisationName() : "";
        notificationService.create(
                event.userId(),
                NotificationType.ROLE_REVOKED,
                "Role revoked",
                "Your " + event.role().name() + " role" + scope + " has been revoked.",
                "ROLE",
                event.userRoleId()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onKycDocumentSubmitted(KycDocumentSubmittedEvent event) {
        notificationService.create(
                event.userId(),
                NotificationType.KYC_DOCUMENT_SUBMITTED,
                "Document submitted",
                "Your " + event.documentType().name() + " has been submitted and is pending review.",
                "KYC",
                event.kycId()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onListingPublished(ListingPublishedEvent event) {
        notificationService.create(
                event.actorUserId(),
                NotificationType.LISTING_PUBLISHED,
                "Listing published",
                "Your " + event.purpose().name().toLowerCase() + " listing is now on the marketplace.",
                "LISTING",
                event.listingId()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLeadCreated(LeadCreatedEvent event) {
        if (event.assignedAgentUserId() != null && !event.assignedAgentUserId().equals(event.inquirerUserId())) {
            notificationService.create(
                    event.assignedAgentUserId(),
                    NotificationType.LEAD_CREATED,
                    "New enquiry",
                    "Someone enquired about one of your listings.",
                    "LEAD",
                    event.leadId()
            );
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onViewingBooked(ViewingBookedEvent event) {
        notificationService.create(
                event.requestedByUserId(),
                NotificationType.VIEWING_BOOKED,
                "Viewing requested",
                "Your viewing request has been sent.",
                "VIEWING",
                event.viewingId()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onApplicationSubmitted(ApplicationSubmittedEvent event) {
        notificationService.create(
                event.applicantUserId(),
                NotificationType.APPLICATION_SUBMITTED,
                "Application submitted",
                "Your application has been sent to the lister.",
                "APPLICATION",
                event.applicationId()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLeaseSigned(LeaseSignedEvent event) {
        notificationService.create(
                event.tenantUserId(),
                NotificationType.LEASE_SIGNED,
                "Lease signed",
                "A lease was e-signed and the property moved under management.",
                "LEASE",
                event.leaseId()
        );
    }
}