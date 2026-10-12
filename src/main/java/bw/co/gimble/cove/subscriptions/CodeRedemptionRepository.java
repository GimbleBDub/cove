package bw.co.gimble.cove.subscriptions;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CodeRedemptionRepository extends JpaRepository<CodeRedemption, UUID> {
    long countByCodeId(UUID codeId);
}
