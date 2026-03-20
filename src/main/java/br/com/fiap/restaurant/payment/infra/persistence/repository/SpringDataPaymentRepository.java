package br.com.fiap.restaurant.payment.infra.persistence.repository;

import br.com.fiap.restaurant.payment.infra.persistence.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataPaymentRepository extends JpaRepository<PaymentEntity, UUID> {

    Optional<PaymentEntity> findByOrderId(Long orderId);

    List<PaymentEntity> findByStatus(String status);

    @Query("""
            select p
            from PaymentEntity p
            where p.status = 'PENDING'
              and p.retryCount < :maxRetryCount
              and (p.nextRetryAt is null or p.nextRetryAt <= :referenceTime)
            """)
    List<PaymentEntity> findRetryablePendingPayments(
            @Param("referenceTime") OffsetDateTime referenceTime,
            @Param("maxRetryCount") int maxRetryCount
    );
}
