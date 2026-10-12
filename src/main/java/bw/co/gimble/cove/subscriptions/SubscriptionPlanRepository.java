package bw.co.gimble.cove.subscriptions;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, UUID> {
    List<SubscriptionPlan> findByActiveTrue();
    List<SubscriptionPlan> findByActiveTrueAndSubjectType(SubscriptionSubjectType subjectType);
    Optional<SubscriptionPlan> findByCode(String code);
    boolean existsByCode(String code);
}
