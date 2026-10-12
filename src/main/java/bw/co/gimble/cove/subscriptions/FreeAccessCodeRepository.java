package bw.co.gimble.cove.subscriptions;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FreeAccessCodeRepository extends JpaRepository<FreeAccessCode, UUID> {
    Optional<FreeAccessCode> findByCodeIgnoreCase(String code);
    List<FreeAccessCode> findAllByOrderByCreatedAtDesc();
}
