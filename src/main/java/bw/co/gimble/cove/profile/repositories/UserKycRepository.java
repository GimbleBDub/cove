package bw.co.gimble.cove.profile.repositories;

import bw.co.gimble.cove.profile.enums.KycDocumentType;
import bw.co.gimble.cove.profile.models.UserKyc;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserKycRepository extends JpaRepository<UserKyc, UUID> {

    List<UserKyc> findByUserId(UUID userId);

    Optional<UserKyc> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByUserIdAndDocumentType(UUID userId, KycDocumentType documentType);
}
