package br.com.fiap.restaurant.payment.infra.messaging.outbound.adapter;

import br.com.fiap.restaurant.payment.core.domain.model.Payment;
import br.com.fiap.restaurant.payment.core.gateway.PaymentEventPublisherGateway;
import br.com.fiap.restaurant.payment.infra.messaging.config.RabbitProperties;
import br.com.fiap.restaurant.payment.infra.messaging.outbound.dto.PaymentEventMessage;
import br.com.fiap.restaurant.payment.infra.messaging.outbound.producer.PaymentEventRabbitProducer;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
@Primary
public class RabbitPaymentEventPublisherAdapter implements PaymentEventPublisherGateway {

    private final PaymentEventRabbitProducer paymentEventRabbitProducer;
    private final RabbitProperties rabbitProperties;

    public RabbitPaymentEventPublisherAdapter(
            PaymentEventRabbitProducer paymentEventRabbitProducer,
            RabbitProperties rabbitProperties
    ) {
        this.paymentEventRabbitProducer = paymentEventRabbitProducer;
        this.rabbitProperties = rabbitProperties;
    }

    @Override
    public void publishApproved(Payment payment) {
        paymentEventRabbitProducer.send(
                rabbitProperties.getPaymentExchange(),
                rabbitProperties.getPaymentApprovedRoutingKey(),
                toMessage(payment)
        );
    }

    @Override
    public void publishPending(Payment payment) {
        paymentEventRabbitProducer.send(
                rabbitProperties.getPaymentExchange(),
                rabbitProperties.getPaymentPendingRoutingKey(),
                toMessage(payment)
        );
    }

    private PaymentEventMessage toMessage(Payment payment) {
        return new PaymentEventMessage(
                payment.getId(),
                payment.getOrderId(),
                payment.getClientId(),
                payment.getAmount(),
                payment.getStatus().name(),
                OffsetDateTime.now()
        );
    }
}
