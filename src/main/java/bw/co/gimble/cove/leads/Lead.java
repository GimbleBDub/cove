package bw.co.gimble.cove.leads;

import bw.co.gimble.cove.common.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "leads", uniqueConstraints = @UniqueConstraint(
        name = "uq_lead_listing_inquirer", columnNames = {"listing_id", "inquirer_user_id"}))
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lead extends Auditable {

    @Column(name = "listing_id", nullable = false)
    private UUID listingId;

    @Column(name = "inquirer_user_id", nullable = false)
    private UUID inquirerUserId;

    @Column(name = "assigned_agent_user_id")
    private UUID assignedAgentUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private LeadStage stage = LeadStage.NEW;
}
