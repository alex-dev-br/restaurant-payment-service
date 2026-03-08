package br.com.fiap.restaurant.payment.infra.messaging.outbound.producer;

import br.com.fiap.restaurant.payment.infra.messaging.outbound.dto.PaymentEventMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.Mockito.*;

class PaymentEventProducerTest {

    private KafkaTemplate<String, PaymentEventMessage> kafkaTemplate;
    private PaymentEventProducer paymentEventProducer;

    @BeforeEach
    void setUp() {
        kafkaTemplate = mock(KafkaTemplate.class);
        paymentEventProducer = new PaymentEventProducer(kafkaTemplate);
    }

    @Test
    void shouldSendMessageUsingKafkaTemplate() {
        PaymentEventMessage message = new PaymentEventMessage(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("10.00"),
                "APPROVED",
                OffsetDateTime.now()
        );

        paymentEventProducer.send("pagamento.aprovado", "key-1", message);

        verify(kafkaTemplate, times(1))
                .send("pagamento.aprovado", "key-1", message);
    }
}
