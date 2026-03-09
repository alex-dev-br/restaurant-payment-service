package br.com.fiap.restaurant.payment.infra.messaging.outbound.adapter;

import br.com.fiap.restaurant.payment.core.gateway.ExternalPaymentProcessorGateway;
import br.com.fiap.restaurant.payment.core.usecase.ProcessPaymentUseCase;
import br.com.fiap.restaurant.payment.infra.messaging.config.RabbitProperties;
import br.com.fiap.restaurant.payment.infra.messaging.outbound.dto.PaymentEventMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "spring.rabbitmq.listener.simple.auto-startup=false"
})
@ActiveProfiles("rabbit")
class RabbitPaymentEventFlowIntegrationTest {

    @Autowired
    private ProcessPaymentUseCase processPaymentUseCase;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RabbitAdmin rabbitAdmin;

    @Autowired
    private RabbitProperties rabbitProperties;

    @MockitoBean
    private ExternalPaymentProcessorGateway externalPaymentProcessorGateway;

    @BeforeEach
    void setUp() {
        purgeQueue(rabbitProperties.getApprovedDebugQueue());
        purgeQueue(rabbitProperties.getPendingDebugQueue());
    }

    @Test
    void shouldPublishApprovedEventToRabbit() {
        UUID orderId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("120.00");

        when(externalPaymentProcessorGateway.process(
                any(UUID.class),
                any(UUID.class),
                any(BigDecimal.class)
        )).thenReturn(true);

        processPaymentUseCase.execute(orderId, clientId, amount);

        PaymentEventMessage approvedMessage =
                receiveMessage(rabbitProperties.getApprovedDebugQueue(), PaymentEventMessage.class);

        assertNotNull(approvedMessage);
        assertNotNull(approvedMessage.paymentId());
        assertEquals(orderId, approvedMessage.orderId());
        assertEquals(clientId, approvedMessage.clientId());
        assertEquals(amount, approvedMessage.amount());
        assertEquals("APPROVED", approvedMessage.status());
        assertNotNull(approvedMessage.occurredAt());

        PaymentEventMessage pendingMessage =
                receiveMessage(rabbitProperties.getPendingDebugQueue(), PaymentEventMessage.class);

        assertNull(pendingMessage);
    }

    @Test
    void shouldPublishPendingEventWhenProcessorFails() {
        UUID orderId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("150.00");

        when(externalPaymentProcessorGateway.process(
                any(UUID.class),
                any(UUID.class),
                any(BigDecimal.class)
        )).thenReturn(false);

        processPaymentUseCase.execute(orderId, clientId, amount);

        PaymentEventMessage pendingMessage =
                receiveMessage(rabbitProperties.getPendingDebugQueue(), PaymentEventMessage.class);

        assertNotNull(pendingMessage);
        assertNotNull(pendingMessage.paymentId());
        assertEquals(orderId, pendingMessage.orderId());
        assertEquals(clientId, pendingMessage.clientId());
        assertEquals(amount, pendingMessage.amount());
        assertEquals("PENDING", pendingMessage.status());
        assertNotNull(pendingMessage.occurredAt());

        PaymentEventMessage approvedMessage =
                receiveMessage(rabbitProperties.getApprovedDebugQueue(), PaymentEventMessage.class);

        assertNull(approvedMessage);
    }

    @Test
    void shouldPublishPendingEventWhenProcessorThrowsException() {
        UUID orderId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("180.00");

        doThrow(new RuntimeException("processor unavailable"))
                .when(externalPaymentProcessorGateway)
                .process(
                        any(UUID.class),
                        any(UUID.class),
                        any(BigDecimal.class)
                );

        processPaymentUseCase.execute(orderId, clientId, amount);

        PaymentEventMessage pendingMessage =
                receiveMessage(rabbitProperties.getPendingDebugQueue(), PaymentEventMessage.class);

        assertNotNull(pendingMessage);
        assertNotNull(pendingMessage.paymentId());
        assertEquals(orderId, pendingMessage.orderId());
        assertEquals(clientId, pendingMessage.clientId());
        assertEquals(amount, pendingMessage.amount());
        assertEquals("PENDING", pendingMessage.status());
        assertNotNull(pendingMessage.occurredAt());

        PaymentEventMessage approvedMessage =
                receiveMessage(rabbitProperties.getApprovedDebugQueue(), PaymentEventMessage.class);

        assertNull(approvedMessage);
    }

    private void purgeQueue(String queueName) {
        rabbitAdmin.purgeQueue(queueName, true);
    }

    private <T> T receiveMessage(String queueName, Class<T> payloadType) {
        Object message = rabbitTemplate.receiveAndConvert(queueName, 5000);
        assertTrue(
                message == null || payloadType.isInstance(message),
                "Mensagem recebida da fila " + queueName + " com tipo inesperado: "
                        + (message == null ? "null" : message.getClass().getName())
        );
        return payloadType.cast(message);
    }
}