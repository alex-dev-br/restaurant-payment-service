package br.com.fiap.restaurant.payment.core.usecase;

import br.com.fiap.restaurant.payment.core.domain.model.OutboxStatus;
import br.com.fiap.restaurant.payment.core.domain.model.Payment;
import br.com.fiap.restaurant.payment.core.domain.model.PaymentEventType;
import br.com.fiap.restaurant.payment.core.domain.model.PaymentStatus;
import br.com.fiap.restaurant.payment.core.gateway.ExternalPaymentProcessorGateway;
import br.com.fiap.restaurant.payment.core.usecase.command.ProcessPaymentCommand;
import br.com.fiap.restaurant.payment.infra.persistence.adapter.PaymentFinalizationAdapter;
import br.com.fiap.restaurant.payment.infra.persistence.adapter.PaymentOutboxPersistenceMapper;
import br.com.fiap.restaurant.payment.infra.persistence.adapter.PaymentPersistenceMapper;
import br.com.fiap.restaurant.payment.infra.persistence.adapter.PaymentRepositoryAdapter;
import br.com.fiap.restaurant.payment.infra.persistence.repository.SpringDataPaymentOutboxRepository;
import br.com.fiap.restaurant.payment.infra.persistence.repository.SpringDataPaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({
        ProcessPaymentUseCaseITConfig.class,
        PaymentRepositoryAdapter.class,
        PaymentPersistenceMapper.class,
        PaymentFinalizationAdapter.class,
        PaymentOutboxPersistenceMapper.class
})
class ProcessPaymentUseCaseIntegrationTest {

    @Autowired
    private ProcessPaymentUseCase processPaymentUseCase;

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
    }

    @Test
    void shouldPersistApprovedPaymentAndCreateApprovedOutboxEventWhenExternalProcessorApproves() {
        Long orderId = 1L;
        UUID clientId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("120.00");

        when(externalPaymentProcessorGateway.process(any(UUID.class), eq(clientId), eq(amount)))
                .thenReturn(true);

        ProcessPaymentCommand command = new ProcessPaymentCommand(orderId, clientId, amount);
        Payment result = processPaymentUseCase.execute(command);

        assertNotNull(result);
        assertEquals(PaymentStatus.APPROVED, result.getStatus());
        assertEquals(0, result.getRetryCount());
        assertNull(result.getLastRetryAt());
        assertNull(result.getNextRetryAt());

        var savedEntity = springDataPaymentRepository.findByOrderId(orderId);
        assertTrue(savedEntity.isPresent());
        assertEquals(PaymentStatus.APPROVED.name(), savedEntity.get().getStatus());
        assertEquals(new BigDecimal("120.00"), savedEntity.get().getAmount());
        assertEquals(clientId, savedEntity.get().getClientId());

        assertEquals(1, springDataPaymentRepository.findAll().size());
        assertEquals(1, springDataPaymentOutboxRepository.findAll().size());

        var outboxEntity = springDataPaymentOutboxRepository.findAll().get(0);
        assertEquals(result.getId(), outboxEntity.getAggregateId());
        assertEquals(PaymentEventType.PAYMENT_APPROVED.name(), outboxEntity.getEventType());
        assertEquals("ex.payment", outboxEntity.getExchangeName());
        assertEquals("payment.approved", outboxEntity.getRoutingKey());
        assertEquals(OutboxStatus.PENDING.name(), outboxEntity.getStatus());
        assertNull(outboxEntity.getPublishedAt());
        assertTrue(outboxEntity.getPayload().contains("\"status\":\"APPROVED\""));

        verify(externalPaymentProcessorGateway, times(1))
                .process(any(UUID.class), eq(clientId), eq(amount));
    }

    @Test
    void shouldPersistPendingPaymentAndCreatePendingOutboxEventWhenExternalProcessorReturnsFalse() {
        Long orderId = 2L;
        UUID clientId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("55.90");

        when(externalPaymentProcessorGateway.process(any(UUID.class), eq(clientId), eq(amount)))
                .thenReturn(false);

        ProcessPaymentCommand command = new ProcessPaymentCommand(orderId, clientId, amount);
        Payment result = processPaymentUseCase.execute(command);

        assertNotNull(result);
        assertEquals(PaymentStatus.PENDING, result.getStatus());
        assertEquals(1, result.getRetryCount());
        assertNotNull(result.getLastRetryAt());
        assertNotNull(result.getNextRetryAt());

        var savedEntity = springDataPaymentRepository.findByOrderId(orderId);
        assertTrue(savedEntity.isPresent());
        assertEquals(PaymentStatus.PENDING.name(), savedEntity.get().getStatus());
        assertEquals(new BigDecimal("55.90"), savedEntity.get().getAmount());
        assertEquals(clientId, savedEntity.get().getClientId());
        assertEquals(1, savedEntity.get().getRetryCount());
        assertNotNull(savedEntity.get().getLastRetryAt());
        assertNotNull(savedEntity.get().getNextRetryAt());

        assertEquals(1, springDataPaymentRepository.findAll().size());
        assertEquals(1, springDataPaymentOutboxRepository.findAll().size());

        var outboxEntity = springDataPaymentOutboxRepository.findAll().get(0);
        assertEquals(result.getId(), outboxEntity.getAggregateId());
        assertEquals(PaymentEventType.PAYMENT_PENDING.name(), outboxEntity.getEventType());
        assertEquals("payment.pending", outboxEntity.getRoutingKey());
        assertEquals(OutboxStatus.PENDING.name(), outboxEntity.getStatus());
        assertTrue(outboxEntity.getPayload().contains("\"status\":\"PENDING\""));

        verify(externalPaymentProcessorGateway, times(1))
                .process(any(UUID.class), eq(clientId), eq(amount));
    }

    @Test
    void shouldPersistPendingPaymentAndCreatePendingOutboxEventWhenExternalProcessorThrowsException() {
        Long orderId = 3L;
        UUID clientId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("77.50");

        when(externalPaymentProcessorGateway.process(any(UUID.class), eq(clientId), eq(amount)))
                .thenThrow(new RuntimeException("Serviço indisponível"));

        ProcessPaymentCommand command = new ProcessPaymentCommand(orderId, clientId, amount);
        Payment result = processPaymentUseCase.execute(command);

        assertNotNull(result);
        assertEquals(PaymentStatus.PENDING, result.getStatus());
        assertEquals(1, result.getRetryCount());
        assertNotNull(result.getLastRetryAt());
        assertNotNull(result.getNextRetryAt());

        var savedEntity = springDataPaymentRepository.findByOrderId(orderId);
        assertTrue(savedEntity.isPresent());
        assertEquals(PaymentStatus.PENDING.name(), savedEntity.get().getStatus());
        assertEquals(new BigDecimal("77.50"), savedEntity.get().getAmount());
        assertEquals(clientId, savedEntity.get().getClientId());

        assertEquals(1, springDataPaymentRepository.findAll().size());
        assertEquals(1, springDataPaymentOutboxRepository.findAll().size());

        var outboxEntity = springDataPaymentOutboxRepository.findAll().get(0);
        assertEquals(PaymentEventType.PAYMENT_PENDING.name(), outboxEntity.getEventType());
        assertEquals("payment.pending", outboxEntity.getRoutingKey());
        assertEquals(OutboxStatus.PENDING.name(), outboxEntity.getStatus());

        verify(externalPaymentProcessorGateway, times(1))
                .process(any(UUID.class), eq(clientId), eq(amount));
    }

    @Test
    void shouldReturnExistingPaymentWithoutCreatingNewOutboxEventWhenPaymentAlreadyExistsForOrder() {
        Long orderId = 4L;
        UUID clientId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("120.00");

        when(externalPaymentProcessorGateway.process(any(UUID.class), eq(clientId), eq(amount)))
                .thenReturn(true);

        ProcessPaymentCommand command = new ProcessPaymentCommand(orderId, clientId, amount);
        Payment firstResult = processPaymentUseCase.execute(command);
        Payment secondResult = processPaymentUseCase.execute(command);

        assertNotNull(firstResult);
        assertNotNull(secondResult);
        assertEquals(firstResult.getId(), secondResult.getId());
        assertEquals(PaymentStatus.APPROVED, secondResult.getStatus());

        assertEquals(1, springDataPaymentRepository.findAll().size());
        assertEquals(1, springDataPaymentOutboxRepository.findAll().size());

        var savedEntity = springDataPaymentRepository.findByOrderId(orderId);
        assertTrue(savedEntity.isPresent());
        assertEquals(PaymentStatus.APPROVED.name(), savedEntity.get().getStatus());
        assertEquals(clientId, savedEntity.get().getClientId());
        assertEquals(amount, savedEntity.get().getAmount());

        verify(externalPaymentProcessorGateway, times(1))
                .process(any(UUID.class), eq(clientId), eq(amount));
    }
}
