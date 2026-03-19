package br.com.fiap.restaurant.payment.core.usecase;

import br.com.fiap.restaurant.payment.core.domain.model.Payment;
import br.com.fiap.restaurant.payment.core.domain.model.PaymentStatus;
import br.com.fiap.restaurant.payment.core.gateway.ExternalPaymentProcessorGateway;
import br.com.fiap.restaurant.payment.core.gateway.PaymentEventPublisherGateway;
import br.com.fiap.restaurant.payment.core.usecase.command.ProcessPaymentCommand;
import br.com.fiap.restaurant.payment.infra.persistence.adapter.PaymentPersistenceMapper;
import br.com.fiap.restaurant.payment.infra.persistence.adapter.PaymentRepositoryAdapter;
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
        PaymentPersistenceMapper.class
})
class ProcessPaymentUseCaseIntegrationTest {

    @Autowired
    private ProcessPaymentUseCase processPaymentUseCase;

    @Autowired
    private SpringDataPaymentRepository springDataPaymentRepository;

    @MockitoBean
    private ExternalPaymentProcessorGateway externalPaymentProcessorGateway;

    @MockitoBean
    private PaymentEventPublisherGateway paymentEventPublisherGateway;

    @BeforeEach
    void setUp() {
        springDataPaymentRepository.deleteAll();
        reset(
                externalPaymentProcessorGateway,
                paymentEventPublisherGateway
        );
    }

    @Test
    void shouldPersistApprovedPaymentWhenExternalProcessorApproves() {
        Long orderId = 1L;
        UUID clientId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("120.00");

        when(externalPaymentProcessorGateway.process(any(UUID.class), eq(clientId), eq(amount)))
                .thenReturn(true);

        ProcessPaymentCommand command = new ProcessPaymentCommand(orderId, clientId, amount);
        Payment result = processPaymentUseCase.execute(command);

        assertNotNull(result);
        assertEquals(PaymentStatus.APPROVED, result.getStatus());

        var savedEntity = springDataPaymentRepository.findByOrderId(orderId);
        assertTrue(savedEntity.isPresent());
        assertEquals(PaymentStatus.APPROVED.name(), savedEntity.get().getStatus());
        assertEquals(new BigDecimal("120.00"), savedEntity.get().getAmount());
        assertEquals(clientId, savedEntity.get().getClientId());

        assertEquals(1, springDataPaymentRepository.findAll().size());

        verify(externalPaymentProcessorGateway, times(1))
                .process(any(UUID.class), eq(clientId), eq(amount));
        verify(paymentEventPublisherGateway, times(1))
                .publishApproved(any(Payment.class));
        verify(paymentEventPublisherGateway, never())
                .publishPending(any(Payment.class));
    }

    @Test
    void shouldPersistPendingPaymentWhenExternalProcessorReturnsFalse() {
        Long orderId = 2L;
        UUID clientId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("55.90");

        when(externalPaymentProcessorGateway.process(any(UUID.class), eq(clientId), eq(amount)))
                .thenReturn(false);

        ProcessPaymentCommand command = new ProcessPaymentCommand(orderId, clientId, amount);
        Payment result = processPaymentUseCase.execute(command);

        assertNotNull(result);
        assertEquals(PaymentStatus.PENDING, result.getStatus());

        var savedEntity = springDataPaymentRepository.findByOrderId(orderId);
        assertTrue(savedEntity.isPresent());
        assertEquals(PaymentStatus.PENDING.name(), savedEntity.get().getStatus());
        assertEquals(new BigDecimal("55.90"), savedEntity.get().getAmount());
        assertEquals(clientId, savedEntity.get().getClientId());

        assertEquals(1, springDataPaymentRepository.findAll().size());

        verify(externalPaymentProcessorGateway, times(1))
                .process(any(UUID.class), eq(clientId), eq(amount));
        verify(paymentEventPublisherGateway, never())
                .publishApproved(any(Payment.class));
        verify(paymentEventPublisherGateway, times(1))
                .publishPending(any(Payment.class));
    }

    @Test
    void shouldPersistPendingPaymentWhenExternalProcessorThrowsException() {
        Long orderId = 3L;
        UUID clientId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("77.50");

        when(externalPaymentProcessorGateway.process(any(UUID.class), eq(clientId), eq(amount)))
                .thenThrow(new RuntimeException("Serviço indisponível"));

        ProcessPaymentCommand command = new ProcessPaymentCommand(orderId, clientId, amount);
        Payment result = processPaymentUseCase.execute(command);

        assertNotNull(result);
        assertEquals(PaymentStatus.PENDING, result.getStatus());

        var savedEntity = springDataPaymentRepository.findByOrderId(orderId);
        assertTrue(savedEntity.isPresent());
        assertEquals(PaymentStatus.PENDING.name(), savedEntity.get().getStatus());
        assertEquals(new BigDecimal("77.50"), savedEntity.get().getAmount());
        assertEquals(clientId, savedEntity.get().getClientId());

        assertEquals(1, springDataPaymentRepository.findAll().size());

        verify(externalPaymentProcessorGateway, times(1))
                .process(any(UUID.class), eq(clientId), eq(amount));
        verify(paymentEventPublisherGateway, never())
                .publishApproved(any(Payment.class));
        verify(paymentEventPublisherGateway, times(1))
                .publishPending(any(Payment.class));
    }

    @Test
    void shouldReturnExistingPaymentWhenPaymentAlreadyExistsForOrder() {
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

        var savedEntity = springDataPaymentRepository.findByOrderId(orderId);
        assertTrue(savedEntity.isPresent());
        assertEquals(PaymentStatus.APPROVED.name(), savedEntity.get().getStatus());
        assertEquals(clientId, savedEntity.get().getClientId());
        assertEquals(amount, savedEntity.get().getAmount());

        verify(externalPaymentProcessorGateway, times(1))
                .process(any(UUID.class), eq(clientId), eq(amount));
        verify(paymentEventPublisherGateway, times(1))
                .publishApproved(any(Payment.class));
        verify(paymentEventPublisherGateway, never())
                .publishPending(any(Payment.class));
    }
}