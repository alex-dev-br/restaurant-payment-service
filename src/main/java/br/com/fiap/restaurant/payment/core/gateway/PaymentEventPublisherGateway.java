package br.com.fiap.restaurant.payment.core.gateway;

import br.com.fiap.restaurant.payment.core.domain.model.Payment;

public interface PaymentEventPublisherGateway {

    void publishApproved(Payment payment);

    void publishPending(Payment payment);

    void publishFailed(Payment payment);
}
