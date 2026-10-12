package bw.co.gimble.cove.payments;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {
    Optional<PaymentTransaction> findByTransToken(String transToken);
    List<PaymentTransaction> findByPayerUserIdOrderByCreatedAtDesc(UUID payerUserId);
    List<PaymentTransaction> findAllByOrderByCreatedAtDesc();
}
