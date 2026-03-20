package br.com.fiap.restaurant.payment.infra.messaging.outbound.adapter;

import br.com.fiap.restaurant.payment.core.domain.model.PaymentOutboxEvent;
import br.com.fiap.restaurant.payment.core.gateway.PaymentEventTransportGateway;
import br.com.fiap.restaurant.payment.infra.messaging.outbound.dto.PaymentEventMessage;
import br.com.fiap.restaurant.payment.infra.messaging.outbound.producer.PaymentEventRabbitProducer;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Component
public class RabbitPaymentEventTransportAdapter implements PaymentEventTransportGateway {

    private final PaymentEventRabbitProducer paymentEventRabbitProducer;
    private final JsonMapper objectMapper;

    public RabbitPaymentEventTransportAdapter(
            PaymentEventRabbitProducer paymentEventRabbitProducer,
            JsonMapper objectMapper
    ) {
        this.paymentEventRabbitProducer = paymentEventRabbitProducer;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(PaymentOutboxEvent event) {
        try {
            PaymentEventMessage message =
                    objectMapper.readValue(event.getPayload(), PaymentEventMessage.class);

            paymentEventRabbitProducer.send(
                    event.getExchangeName(),
                    event.getRoutingKey(),
                    message
            );
        } catch (JacksonException exception) {
            throw new IllegalStateException("Erro ao desserializar payload do outbox", exception);
        }
    }
}
