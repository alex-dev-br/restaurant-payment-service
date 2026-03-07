package br.com.fiap.restaurant.payment.core.domain.gateway;

import br.com.fiap.restaurant.payment.core.domain.model.Payment;

public interface PaymentEventPublisherGateway {

    void publishPaymentApproved(Payment payment);

    void publishPaymentPending(Payment payment);
}
