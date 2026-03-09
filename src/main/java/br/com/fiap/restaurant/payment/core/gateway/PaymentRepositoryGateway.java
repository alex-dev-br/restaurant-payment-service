package br.com.fiap.restaurant.payment.core.gateway;

import br.com.fiap.restaurant.payment.core.domain.model.Payment;
import br.com.fiap.restaurant.payment.core.domain.model.PaymentStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepositoryGateway {

    Payment save(Payment payment);
    Optional<Payment> findById(UUID id);
    Optional<Payment> findByOrderId(UUID orderId);
    List<Payment> findByStatus(PaymentStatus status);
}
