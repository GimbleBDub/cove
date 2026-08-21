package bw.co.gimble.cove.notifications;

import bw.co.gimble.cove.events.*;
import bw.co.gimble.cove.role.Role;
import bw.co.gimble.cove.role.RoleStatus;
import bw.co.gimble.cove.role.UserRole;
import bw.co.gimble.cove.role.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
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
}