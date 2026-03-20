package br.com.fiap.restaurant.payment.infra.persistence.repository;

import br.com.fiap.restaurant.payment.infra.persistence.entity.PaymentOutboxEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataPaymentOutboxRepository extends JpaRepository<PaymentOutboxEntity, UUID> {

    List<PaymentOutboxEntity> findByStatusOrderByCreatedAtAsc(String status, Pageable pageable);
}
