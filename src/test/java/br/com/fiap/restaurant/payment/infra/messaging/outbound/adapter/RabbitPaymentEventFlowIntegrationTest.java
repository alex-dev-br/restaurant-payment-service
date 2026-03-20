package br.com.fiap.restaurant.payment.infra.messaging.outbound.adapter;

import br.com.fiap.restaurant.payment.core.domain.model.Payment;
import br.com.fiap.restaurant.payment.core.domain.model.PaymentStatus;
import br.com.fiap.restaurant.payment.core.gateway.ExternalPaymentProcessorGateway;
import br.com.fiap.restaurant.payment.core.gateway.PaymentRepositoryGateway;
import br.com.fiap.restaurant.payment.core.usecase.ProcessPaymentUseCase;
import br.com.fiap.restaurant.payment.core.usecase.PublishPendingPaymentOutboxUseCase;
import br.com.fiap.restaurant.payment.core.usecase.RetryPendingPaymentsUseCase;
import br.com.fiap.restaurant.payment.core.usecase.command.ProcessPaymentCommand;
import br.com.fiap.restaurant.payment.infra.messaging.config.RabbitProperties;
import br.com.fiap.restaurant.payment.infra.messaging.outbound.dto.PaymentEventMessage;
import br.com.fiap.restaurant.payment.infra.persistence.repository.SpringDataPaymentOutboxRepository;
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
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "app.payment.retry.scheduler.enabled=false",
        "app.payment.outbox.publisher.enabled=false"
})
@ActiveProfiles("test")
class RabbitPaymentEventFlowIntegrationTest {

    @Autowired
    private ProcessPaymentUseCase processPaymentUseCase;

    @Autowired
    private RetryPendingPaymentsUseCase retryPendingPaymentsUseCase;

    @Autowired
    private PublishPendingPaymentOutboxUseCase publishPendingPaymentOutboxUseCase;

    @Autowired
    private PaymentRepositoryGateway paymentRepositoryGateway;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private RabbitProperties rabbitProperties;

    @Autowired
    private SpringDataPaymentRepository springDataPaymentRepository;

    @Autowired
    private SpringDataPaymentOutboxRepository springDataPaymentOutboxRepository;

    @MockitoBean
    private ExternalPaymentProcessorGateway externalPaymentProcessorGateway;

    @BeforeEach
    void setUp() {
        springDataPaymentOutboxRepository.deleteAll();
        springDataPaymentRepository.deleteAll();
        reset(externalPaymentProcessorGateway);

        purgeQueue(rabbitProperties.getQueue().getPaymentApprovedDebug());
        purgeQueue(rabbitProperties.getQueue().getPaymentPendingDebug());
        purgeQueue(rabbitProperties.getQueue().getPaymentFailedDebug());
    }

    @Test
    void shouldPublishApprovedEventToRabbitFromOutbox() {
        Long orderId = nextOrderId();
        UUID clientId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("120.00");

        when(externalPaymentProcessorGateway.process(
                any(UUID.class),
                any(UUID.class),
                any(BigDecimal.class)
        )).thenReturn(true);

        ProcessPaymentCommand command = new ProcessPaymentCommand(orderId, clientId, amount);
        processPaymentUseCase.execute(command);
        publishPendingPaymentOutboxUseCase.execute();

        PaymentEventMessage approvedMessage = receiveExpectedMessage(
                rabbitProperties.getQueue().getPaymentApprovedDebug(),
                PaymentEventMessage.class
        );

        assertPaymentEvent(approvedMessage, orderId, clientId, amount, "APPROVED");

        PaymentEventMessage pendingMessage = receiveUnexpectedMessage(
                rabbitProperties.getQueue().getPaymentPendingDebug(),
                PaymentEventMessage.class
        );
        PaymentEventMessage failedMessage = receiveUnexpectedMessage(
                rabbitProperties.getQueue().getPaymentFailedDebug(),
                PaymentEventMessage.class
        );

        assertNull(pendingMessage);
        assertNull(failedMessage);
    }

    @Test
    void shouldPublishPendingEventToRabbitFromOutboxWhenInitialProcessingFails() {
        Long orderId = nextOrderId();
        UUID clientId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("150.00");

        when(externalPaymentProcessorGateway.process(
                any(UUID.class),
                any(UUID.class),
                any(BigDecimal.class)
        )).thenReturn(false);

        ProcessPaymentCommand command = new ProcessPaymentCommand(orderId, clientId, amount);
        processPaymentUseCase.execute(command);
        publishPendingPaymentOutboxUseCase.execute();

        PaymentEventMessage pendingMessage = receiveExpectedMessage(
                rabbitProperties.getQueue().getPaymentPendingDebug(),
                PaymentEventMessage.class
        );

        assertPaymentEvent(pendingMessage, orderId, clientId, amount, "PENDING");

        PaymentEventMessage approvedMessage = receiveUnexpectedMessage(
                rabbitProperties.getQueue().getPaymentApprovedDebug(),
                PaymentEventMessage.class
        );
        PaymentEventMessage failedMessage = receiveUnexpectedMessage(
                rabbitProperties.getQueue().getPaymentFailedDebug(),
                PaymentEventMessage.class
        );

        assertNull(approvedMessage);
        assertNull(failedMessage);
    }

    @Test
    void shouldPublishPendingEventToRabbitFromOutboxWhenInitialProcessingThrowsException() {
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

        ProcessPaymentCommand command = new ProcessPaymentCommand(orderId, clientId, amount);
        processPaymentUseCase.execute(command);
        publishPendingPaymentOutboxUseCase.execute();

        PaymentEventMessage pendingMessage = receiveExpectedMessage(
                rabbitProperties.getQueue().getPaymentPendingDebug(),
                PaymentEventMessage.class
        );

        assertPaymentEvent(pendingMessage, orderId, clientId, amount, "PENDING");

        PaymentEventMessage approvedMessage = receiveUnexpectedMessage(
                rabbitProperties.getQueue().getPaymentApprovedDebug(),
                PaymentEventMessage.class
        );
        PaymentEventMessage failedMessage = receiveUnexpectedMessage(
                rabbitProperties.getQueue().getPaymentFailedDebug(),
                PaymentEventMessage.class
        );

        assertNull(approvedMessage);
        assertNull(failedMessage);
    }

    @Test
    void shouldPublishFailedEventToRabbitFromOutboxWhenRetryAttemptsAreExhausted() {
        Long orderId = nextOrderId();
        UUID clientId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("210.00");

        Payment payment = new Payment(
                UUID.randomUUID(),
                orderId,
                clientId,
                amount,
                PaymentStatus.PENDING,
                OffsetDateTime.now().minusMinutes(2),
                OffsetDateTime.now().minusMinutes(2),
                2,
                OffsetDateTime.now().minusMinutes(1),
                OffsetDateTime.now().minusSeconds(1)
        );

        paymentRepositoryGateway.save(payment);

        when(externalPaymentProcessorGateway.process(
                any(UUID.class),
                any(UUID.class),
                any(BigDecimal.class)
        )).thenReturn(false);

        retryPendingPaymentsUseCase.execute();
        publishPendingPaymentOutboxUseCase.execute();

        PaymentEventMessage failedMessage = receiveExpectedMessage(
                rabbitProperties.getQueue().getPaymentFailedDebug(),
                PaymentEventMessage.class
        );

        assertPaymentEvent(failedMessage, orderId, clientId, amount, "FAILED");

        PaymentEventMessage approvedMessage = receiveUnexpectedMessage(
                rabbitProperties.getQueue().getPaymentApprovedDebug(),
                PaymentEventMessage.class
        );
        PaymentEventMessage pendingMessage = receiveUnexpectedMessage(
                rabbitProperties.getQueue().getPaymentPendingDebug(),
                PaymentEventMessage.class
        );

        assertNull(approvedMessage);
        assertNull(pendingMessage);
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
