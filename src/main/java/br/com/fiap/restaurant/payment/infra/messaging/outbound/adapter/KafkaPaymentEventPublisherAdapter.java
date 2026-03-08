package br.com.fiap.restaurant.payment.infra.messaging.outbound.adapter;

import br.com.fiap.restaurant.payment.core.domain.model.Payment;
import br.com.fiap.restaurant.payment.core.gateway.PaymentEventPublisherGateway;
import br.com.fiap.restaurant.payment.infra.messaging.config.KafkaTopicsProperties;
import br.com.fiap.restaurant.payment.infra.messaging.outbound.dto.PaymentEventMessage;
import br.com.fiap.restaurant.payment.infra.messaging.outbound.producer.PaymentEventProducer;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
@Primary
public class KafkaPaymentEventPublisherAdapter implements PaymentEventPublisherGateway {
    private final PaymentEventProducer paymentEventProducer;
    private final KafkaTopicsProperties kafkaTopicsProperties;

    public KafkaPaymentEventPublisherAdapter(
            PaymentEventProducer paymentEventProducer,
            KafkaTopicsProperties kafkaTopicsProperties
    ) {
        this.paymentEventProducer = paymentEventProducer;
        this.kafkaTopicsProperties = kafkaTopicsProperties;
    }

    @Override
    public void publishApproved(Payment payment) {
        paymentEventProducer.send(
                kafkaTopicsProperties.getPaymentApproved(),
                payment.getOrderId().toString(),
                toMessage(payment)
        );
    }

    @Override
    public void publishPending(Payment payment) {
        paymentEventProducer.send(
                kafkaTopicsProperties.getPaymentPending(),
                payment.getOrderId().toString(),
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


