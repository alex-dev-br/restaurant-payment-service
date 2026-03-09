package br.com.fiap.restaurant.payment.infra.messaging.outbound.producer;

import br.com.fiap.restaurant.payment.infra.messaging.outbound.dto.PaymentEventMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventRabbitProducer {

    private final RabbitTemplate rabbitTemplate;

    public PaymentEventRabbitProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void send(String exchange, String routingKey, PaymentEventMessage message) {
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
    }
}
