package br.com.fiap.restaurant.payment.core.gateway;

import br.com.fiap.restaurant.payment.core.domain.model.Payment;

public interface PaymentFinalizationGateway {

    Payment saveApprovedAndEnqueue(Payment payment);

    Payment savePendingAndEnqueue(Payment payment);

    Payment saveFailedAndEnqueue(Payment payment);
}