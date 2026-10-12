package bw.co.gimble.cove.common;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLogEntry, UUID> {
    List<AuditLogEntry> findByEntityTypeAndEntityIdOrderByOccurredAtDesc(String entityType, UUID entityId);
}
