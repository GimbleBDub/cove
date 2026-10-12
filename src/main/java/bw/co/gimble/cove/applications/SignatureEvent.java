package bw.co.gimble.cove.applications;

import bw.co.gimble.cove.common.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "signature_events")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignatureEvent extends Auditable {

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(name = "signer_user_id", nullable = false)
    private UUID signerUserId;

    @Column(nullable = false)
    private String party; // APPLICANT or OWNER

    private String typedName;

    private String imageKey;

    private String imageUrl;

    @Column(nullable = false)
    private String sha256;
}
