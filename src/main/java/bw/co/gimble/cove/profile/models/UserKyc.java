package bw.co.gimble.cove.profile.models;

import bw.co.gimble.cove.common.Auditable;
import bw.co.gimble.cove.profile.enums.KycDocumentType;
import bw.co.gimble.cove.profile.enums.KycStatus;
import bw.co.gimble.cove.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "user_kyc")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserKyc extends Auditable {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KycDocumentType documentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KycStatus status;

    @Column(nullable = false)
    private String fileUrl;

    private String documentNumber;

    private LocalDate issueDate;

    private LocalDate expiryDate;

    private LocalDate verifiedAt;

    private LocalDate rejectedAt;

    private String rejectionReason;

    private String verifiedBy;
}