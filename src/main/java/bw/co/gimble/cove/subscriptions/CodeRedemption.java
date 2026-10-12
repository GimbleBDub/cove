package bw.co.gimble.cove.subscriptions;

import bw.co.gimble.cove.common.Auditable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/** Immutable audit row - one per successful redemption. */
@Getter
@Setter
@Entity
@Table(name = "code_redemptions")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodeRedemption extends Auditable {

    @Column(name = "code_id", nullable = false)
    private UUID codeId;

    @Column(name = "redeemed_by_user_id", nullable = false)
    private UUID redeemedByUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "subject_type", nullable = false)
    private SubscriptionSubjectType subjectType;

    @Column(name = "subject_id", nullable = false)
    private UUID subjectId;

    @Column(name = "subscription_id", nullable = false)
    private UUID subscriptionId;
}
