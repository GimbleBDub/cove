package bw.co.gimble.cove.profile.repositories;

import bw.co.gimble.cove.profile.models.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    Optional<UserProfile> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);
}