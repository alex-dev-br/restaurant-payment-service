package br.com.fiap.restaurant.payment.infra.messaging.outbound.adapter;

import br.com.fiap.restaurant.payment.core.domain.model.Payment;
import br.com.fiap.restaurant.payment.core.gateway.PaymentEventPublisherGateway;
import br.com.fiap.restaurant.payment.infra.messaging.config.RabbitProperties;
import br.com.fiap.restaurant.payment.infra.messaging.dto.EventDTO;
import br.com.fiap.restaurant.payment.infra.messaging.outbound.dto.PaymentEventMessage;
import br.com.fiap.restaurant.payment.infra.messaging.outbound.producer.PaymentEventRabbitProducer;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
@Primary
public class RabbitPaymentEventPublisherAdapter implements PaymentEventPublisherGateway {

    public static final String PAYMENT_APPROVED_EVENT_TYPE = "payment.approved";
    public static final String PAYMENT_PENDING_EVENT_TYPE = "payment.pending";
    public static final String PAYMENT_FAILED_EVENT_TYPE = "payment.failed";
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
                rabbitProperties.getExchange().getPayment(),
                rabbitProperties.getRoutingKey().getPaymentApproved(),
                new EventDTO<>(PAYMENT_APPROVED_EVENT_TYPE, toMessage(payment))
        );
    }

    @Override
    public void publishPending(Payment payment) {
        paymentEventRabbitProducer.send(
                rabbitProperties.getExchange().getPayment(),
                rabbitProperties.getRoutingKey().getPaymentPending(),
                new EventDTO<>(PAYMENT_PENDING_EVENT_TYPE, toMessage(payment))
        );
    }

    @Override
    public void publishFailed(Payment payment) {
        paymentEventRabbitProducer.send(
                rabbitProperties.getExchange().getPayment(),
                rabbitProperties.getRoutingKey().getPaymentFailed(),
                new EventDTO<>(PAYMENT_FAILED_EVENT_TYPE, toMessage(payment))
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
