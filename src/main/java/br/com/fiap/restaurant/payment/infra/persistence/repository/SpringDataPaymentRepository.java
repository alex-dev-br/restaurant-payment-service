package br.com.fiap.restaurant.payment.infra.persistence.repository;

import br.com.fiap.restaurant.payment.infra.persistence.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataPaymentRepository extends JpaRepository<PaymentEntity, UUID> {

    Optional<PaymentEntity> findByOrderId(UUID orderId);
    List<PaymentEntity> findByStatus(String status);
}
