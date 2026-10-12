package bw.co.gimble.cove.subscriptions;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    Optional<Subscription> findFirstBySubjectTypeAndSubjectIdAndStatusOrderByStartsAtDesc(
            SubscriptionSubjectType subjectType, UUID subjectId, SubscriptionStatus status);

    List<Subscription> findBySubjectTypeAndSubjectIdOrderByStartsAtDesc(
            SubscriptionSubjectType subjectType, UUID subjectId);

    List<Subscription> findAllByOrderByStartsAtDesc();

    List<Subscription> findByStatus(SubscriptionStatus status);
}
