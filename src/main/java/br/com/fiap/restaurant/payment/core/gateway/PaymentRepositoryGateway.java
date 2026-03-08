package br.com.fiap.restaurant.payment.core.gateway;

import br.com.fiap.restaurant.payment.core.domain.model.Payment;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepositoryGateway {

    Payment save(Payment payment);
    Optional<Payment> findByOrderId(UUID orderId);
}
