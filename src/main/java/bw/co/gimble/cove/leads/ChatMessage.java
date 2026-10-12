package bw.co.gimble.cove.leads;

import bw.co.gimble.cove.common.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "chat_messages")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage extends Auditable {

    @Column(name = "lead_id", nullable = false)
    private UUID leadId;

    @Column(name = "sender_user_id", nullable = false)
    private UUID senderUserId;

    @Column(nullable = false, length = 4000)
    private String body;
}
