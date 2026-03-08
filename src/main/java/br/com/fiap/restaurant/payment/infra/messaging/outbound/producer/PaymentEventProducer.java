package br.com.fiap.restaurant.payment.infra.messaging.outbound.producer;

import br.com.fiap.restaurant.payment.infra.messaging.outbound.dto.PaymentEventMessage;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventProducer {

    private final KafkaTemplate<String, PaymentEventMessage> kafkaTemplate;

    public PaymentEventProducer(KafkaTemplate<String, PaymentEventMessage> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void send(String topic, String key, PaymentEventMessage message) {
        kafkaTemplate.send(topic, key, message);
    }

}
