package bw.co.gimble.cove.common;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void record(String entityType, UUID entityId, String action,
                       String fromValue, String toValue, UUID actorUserId, String note) {
        auditLogRepository.save(AuditLogEntry.builder()
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .fromValue(fromValue)
                .toValue(toValue)
                .actorUserId(actorUserId)
                .occurredAt(Instant.now())
                .note(note)
                .build());
    }
}
