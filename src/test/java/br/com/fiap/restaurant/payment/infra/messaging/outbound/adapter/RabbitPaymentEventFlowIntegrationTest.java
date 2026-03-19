package br.com.fiap.restaurant.payment.infra.messaging.outbound.adapter;

import br.com.fiap.restaurant.payment.core.gateway.ExternalPaymentProcessorGateway;
import br.com.fiap.restaurant.payment.core.usecase.ProcessPaymentUseCase;
import br.com.fiap.restaurant.payment.infra.messaging.config.RabbitProperties;
import br.com.fiap.restaurant.payment.infra.messaging.outbound.dto.PaymentEventMessage;
import br.com.fiap.restaurant.payment.infra.persistence.repository.SpringDataPaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
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
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "app.payment.retry.scheduler.enabled=false"
})
@ActiveProfiles("test")
class RabbitPaymentEventFlowIntegrationTest {

    @Autowired
    private ProcessPaymentUseCase processPaymentUseCase;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private RabbitProperties rabbitProperties;

    @Autowired
    private SpringDataPaymentRepository springDataPaymentRepository;

    @MockitoBean
    private ExternalPaymentProcessorGateway externalPaymentProcessorGateway;

    @BeforeEach
    void setUp() {
        springDataPaymentRepository.deleteAll();
        reset(externalPaymentProcessorGateway);

        purgeQueue(rabbitProperties.getQueue().getPaymentApprovedDebug());
        purgeQueue(rabbitProperties.getQueue().getPaymentPendingDebug());
    }

    @Test
    void shouldPublishApprovedEventToRabbit() {
        Long orderId = nextOrderId();
        UUID clientId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("120.00");

        when(externalPaymentProcessorGateway.process(
                any(UUID.class),
                any(UUID.class),
                any(BigDecimal.class)
        )).thenReturn(true);

        processPaymentUseCase.execute(orderId, clientId, amount);

        PaymentEventMessage approvedMessage = receiveExpectedMessage(
                rabbitProperties.getQueue().getPaymentApprovedDebug(),
                PaymentEventMessage.class
        );

        assertPaymentEvent(approvedMessage, orderId, clientId, amount, "APPROVED");

        PaymentEventMessage pendingMessage = receiveUnexpectedMessage(
                rabbitProperties.getQueue().getPaymentPendingDebug(),
                PaymentEventMessage.class
        );

        assertNull(pendingMessage);
    }

    @Test
    void shouldPublishPendingEventWhenProcessorFails() {
        Long orderId = nextOrderId();
        UUID clientId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("150.00");

        when(externalPaymentProcessorGateway.process(
                any(UUID.class),
                any(UUID.class),
                any(BigDecimal.class)
        )).thenReturn(false);

        processPaymentUseCase.execute(orderId, clientId, amount);

        PaymentEventMessage pendingMessage = receiveExpectedMessage(
                rabbitProperties.getQueue().getPaymentPendingDebug(),
                PaymentEventMessage.class
        );

        assertPaymentEvent(pendingMessage, orderId, clientId, amount, "PENDING");

        PaymentEventMessage approvedMessage = receiveUnexpectedMessage(
                rabbitProperties.getQueue().getPaymentApprovedDebug(),
                PaymentEventMessage.class
        );

        assertNull(approvedMessage);
    }

    @Test
    void shouldPublishPendingEventWhenProcessorThrowsException() {
        Long orderId = nextOrderId();
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

        PaymentEventMessage pendingMessage = receiveExpectedMessage(
                rabbitProperties.getQueue().getPaymentPendingDebug(),
                PaymentEventMessage.class
        );

        assertPaymentEvent(pendingMessage, orderId, clientId, amount, "PENDING");

        PaymentEventMessage approvedMessage = receiveUnexpectedMessage(
                rabbitProperties.getQueue().getPaymentApprovedDebug(),
                PaymentEventMessage.class
        );

        assertNull(approvedMessage);
    }

    private Long nextOrderId() {
        return Math.abs(UUID.randomUUID().getMostSignificantBits());
    }

    private void assertPaymentEvent(
            PaymentEventMessage message,
            Long expectedOrderId,
            UUID expectedClientId,
            BigDecimal expectedAmount,
            String expectedStatus
    ) {
        assertNotNull(message);
        assertNotNull(message.paymentId());
        assertEquals(expectedOrderId, message.orderId());
        assertEquals(expectedClientId, message.clientId());
        assertEquals(expectedAmount, message.amount());
        assertEquals(expectedStatus, message.status());
        assertNotNull(message.occurredAt());
    }

    private void purgeQueue(String queueName) {
        amqpAdmin.purgeQueue(queueName, true);
    }

    private <T> T receiveExpectedMessage(String queueName, Class<T> payloadType) {
        Object message = rabbitTemplate.receiveAndConvert(queueName, 5000);

        assertNotNull(message, "Nenhuma mensagem recebida da fila " + queueName);
        assertTrue(
                payloadType.isInstance(message),
                "Mensagem recebida da fila " + queueName + " com tipo inesperado: "
                        + message.getClass().getName()
        );

        return payloadType.cast(message);
    }

    private <T> T receiveUnexpectedMessage(String queueName, Class<T> payloadType) {
        Object message = rabbitTemplate.receiveAndConvert(queueName, 100);

        assertTrue(
                message == null || payloadType.isInstance(message),
                "Mensagem recebida da fila " + queueName + " com tipo inesperado: "
                        + (message == null ? "null" : message.getClass().getName())
        );

        return message == null ? null : payloadType.cast(message);
    }
}