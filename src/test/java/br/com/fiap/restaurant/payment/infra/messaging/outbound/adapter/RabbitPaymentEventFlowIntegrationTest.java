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
import br.com.fiap.restaurant.payment.infra.messaging.dto.EventDTO;
import br.com.fiap.restaurant.payment.infra.messaging.outbound.dto.PaymentEventMessage;
import br.com.fiap.restaurant.payment.infra.persistence.repository.SpringDataPaymentOutboxRepository;
import br.com.fiap.restaurant.payment.infra.persistence.repository.SpringDataPaymentRepository;
import br.com.fiap.restaurant.payment.support.AbstractMessagingIntegrationTest;
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
import java.util.Map;
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
class RabbitPaymentEventFlowIntegrationTest extends AbstractMessagingIntegrationTest {

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

        EventDTO<?> approvedEvent = receiveExpectedEvent(
                rabbitProperties.getQueue().getPaymentApprovedDebug()
        );

        assertPaymentEvent(approvedEvent, "payment.approved", orderId, clientId, amount, "APPROVED");

        EventDTO<?> pendingEvent = receiveUnexpectedEvent(
                rabbitProperties.getQueue().getPaymentPendingDebug()
        );
        EventDTO<?> failedEvent = receiveUnexpectedEvent(
                rabbitProperties.getQueue().getPaymentFailedDebug()
        );

        assertNull(pendingEvent);
        assertNull(failedEvent);
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

        EventDTO<?> pendingEvent = receiveExpectedEvent(
                rabbitProperties.getQueue().getPaymentPendingDebug()
        );

        assertPaymentEvent(pendingEvent, "payment.pending", orderId, clientId, amount, "PENDING");

        EventDTO<?> approvedEvent = receiveUnexpectedEvent(
                rabbitProperties.getQueue().getPaymentApprovedDebug()
        );
        EventDTO<?> failedEvent = receiveUnexpectedEvent(
                rabbitProperties.getQueue().getPaymentFailedDebug()
        );

        assertNull(approvedEvent);
        assertNull(failedEvent);
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

        EventDTO<?> pendingEvent = receiveExpectedEvent(
                rabbitProperties.getQueue().getPaymentPendingDebug()
        );

        assertPaymentEvent(pendingEvent, "payment.pending", orderId, clientId, amount, "PENDING");

        EventDTO<?> approvedEvent = receiveUnexpectedEvent(
                rabbitProperties.getQueue().getPaymentApprovedDebug()
        );
        EventDTO<?> failedEvent = receiveUnexpectedEvent(
                rabbitProperties.getQueue().getPaymentFailedDebug()
        );

        assertNull(approvedEvent);
        assertNull(failedEvent);
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

        EventDTO<?> failedEvent = receiveExpectedEvent(
                rabbitProperties.getQueue().getPaymentFailedDebug()
        );

        assertPaymentEvent(failedEvent, "payment.failed", orderId, clientId, amount, "FAILED");

        EventDTO<?> approvedEvent = receiveUnexpectedEvent(
                rabbitProperties.getQueue().getPaymentApprovedDebug()
        );
        EventDTO<?> pendingEvent = receiveUnexpectedEvent(
                rabbitProperties.getQueue().getPaymentPendingDebug()
        );

        assertNull(approvedEvent);
        assertNull(pendingEvent);
    }

    private Long nextOrderId() {
        return Math.abs(UUID.randomUUID().getMostSignificantBits());
    }

    private void assertPaymentEvent(
            EventDTO<?> event,
            String expectedType,
            Long expectedOrderId,
            UUID expectedClientId,
            BigDecimal expectedAmount,
            String expectedStatus
    ) {
        assertNotNull(event);
        assertNotNull(event.uuid());
        assertEquals(expectedType, event.type());
        assertNotNull(event.createTimeStamp());

        PaymentEventMessage message = toPaymentEventMessage(event.body());

        assertNotNull(message.paymentId());
        assertEquals(expectedOrderId, message.orderId());
        assertEquals(expectedClientId, message.clientId());
        assertEquals(0, expectedAmount.compareTo(message.amount()));
        assertEquals(expectedStatus, message.status());
        assertNotNull(message.occurredAt());
    }

    private PaymentEventMessage toPaymentEventMessage(Object body) {
        assertNotNull(body, "Body do evento não pode ser nulo");

        if (body instanceof PaymentEventMessage paymentEventMessage) {
            return paymentEventMessage;
        }

        assertTrue(
                body instanceof Map<?, ?>,
                "Body do evento com tipo inesperado: " + body.getClass().getName()
        );

        Map<?, ?> map = (Map<?, ?>) body;

        return new PaymentEventMessage(
                UUID.fromString(requiredString(map, "paymentId")),
                requiredLong(map, "orderId"),
                UUID.fromString(requiredString(map, "clientId")),
                requiredBigDecimal(map, "amount"),
                requiredString(map, "status"),
                OffsetDateTime.parse(requiredString(map, "occurredAt"))
        );
    }

    private String requiredString(Map<?, ?> map, String key) {
        Object value = map.get(key);
        assertNotNull(value, "Campo ausente no body do evento: " + key);
        return value.toString();
    }

    private Long requiredLong(Map<?, ?> map, String key) {
        Object value = map.get(key);
        assertNotNull(value, "Campo ausente no body do evento: " + key);

        if (value instanceof Number number) {
            return number.longValue();
        }

        return Long.parseLong(value.toString());
    }

    private BigDecimal requiredBigDecimal(Map<?, ?> map, String key) {
        Object value = map.get(key);
        assertNotNull(value, "Campo ausente no body do evento: " + key);
        return new BigDecimal(value.toString());
    }

    private void purgeQueue(String queueName) {
        amqpAdmin.purgeQueue(queueName, true);
    }

    private EventDTO<?> receiveExpectedEvent(String queueName) {
        Object message = rabbitTemplate.receiveAndConvert(queueName, 5000);

        assertNotNull(message, "Nenhuma mensagem recebida da fila " + queueName);
        assertTrue(
                message instanceof EventDTO<?>,
                "Mensagem recebida da fila " + queueName + " com tipo inesperado: "
                        + message.getClass().getName()
        );

        return (EventDTO<?>) message;
    }

    private EventDTO<?> receiveUnexpectedEvent(String queueName) {
        Object message = rabbitTemplate.receiveAndConvert(queueName, 100);

        assertTrue(
                message == null || message instanceof EventDTO<?>,
                "Mensagem recebida da fila " + queueName + " com tipo inesperado: "
                        + (message == null ? "null" : message.getClass().getName())
        );

        return message == null ? null : (EventDTO<?>) message;
    }
}
