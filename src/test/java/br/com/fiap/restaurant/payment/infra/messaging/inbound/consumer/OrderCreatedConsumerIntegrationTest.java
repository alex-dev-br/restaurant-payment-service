package br.com.fiap.restaurant.payment.infra.messaging.inbound.consumer;

import br.com.fiap.restaurant.payment.core.usecase.ProcessPaymentUseCase;
import br.com.fiap.restaurant.payment.infra.messaging.inbound.dto.OrderCreatedMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest
@EmbeddedKafka(
        partitions = 1,
        topics = { "pedido.criado" }
)
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "app.kafka.topics.order-created=pedido.criado",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
        "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer"
})
class OrderCreatedConsumerIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @MockitoBean
    private ProcessPaymentUseCase processPaymentUseCase;

    @Test
    void shouldConsumeOrderCreatedEventAndCallProcessPaymentUseCase() {
        UUID orderId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("120.00");

        OrderCreatedMessage message = new OrderCreatedMessage(orderId, clientId, amount);

        kafkaTemplate.send("pedido.criado", orderId.toString(), message);

        verify(processPaymentUseCase, timeout(5000))
                .execute(orderId, clientId, amount);
    }
}