package br.com.fiap.restaurant.payment.core.gateway;

import br.com.fiap.restaurant.payment.core.domain.model.PaymentOutboxEvent;

public interface PaymentEventTransportGateway {

    void publish(PaymentOutboxEvent event);
}
