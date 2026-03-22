package br.com.fiap.restaurant.payment.infra.messaging.outbound.producer;

import br.com.fiap.restaurant.payment.infra.messaging.dto.EventDTO;
import br.com.fiap.restaurant.payment.infra.messaging.outbound.dto.PaymentEventMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventRabbitProducer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventRabbitProducer.class);

    private final RabbitTemplate rabbitTemplate;

    public PaymentEventRabbitProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void send(String exchange, String routingKey, EventDTO<PaymentEventMessage> message) {
        log.info("Sending event: {}", message);
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
    }
}
