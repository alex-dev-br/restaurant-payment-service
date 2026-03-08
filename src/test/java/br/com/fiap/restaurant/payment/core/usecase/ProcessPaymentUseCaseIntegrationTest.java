package br.com.fiap.restaurant.payment.core.usecase;

import br.com.fiap.restaurant.payment.core.domain.gateway.ExternalPaymentProcessorGateway;
import br.com.fiap.restaurant.payment.core.domain.gateway.PaymentEventPublisherGateway;
import br.com.fiap.restaurant.payment.core.domain.model.Payment;
import br.com.fiap.restaurant.payment.core.domain.model.PaymentStatus;
import br.com.fiap.restaurant.payment.infra.persistence.repository.SpringDataPaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        ProcessPaymentUseCaseITConfig.class,
        br.com.fiap.restaurant.payment.infra.persistence.adapter.PaymentRepositoryAdapter.class,
        br.com.fiap.restaurant.payment.infra.persistence.adapter.PaymentPersistenceMapper.class
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
    }

    @Test
    void shouldPersistApprovedPaymentWhenExternalProcessorApproves() {
        UUID orderId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("120.00");

        when(externalPaymentProcessorGateway.process(any(UUID.class), eq(clientId), eq(amount)))
                .thenReturn(true);

        Payment result = processPaymentUseCase.execute(orderId, clientId, amount);

        assertNotNull(result);
        assertEquals(PaymentStatus.APPROVED, result.getStatus());

        var savedEntity = springDataPaymentRepository.findByOrderId(orderId);
        assertTrue(savedEntity.isPresent());
        assertEquals("APPROVED", savedEntity.get().getStatus());
        assertEquals(new BigDecimal("120.00"), savedEntity.get().getAmount());
        assertEquals(clientId, savedEntity.get().getClientId());

        verify(paymentEventPublisherGateway, times(1)).publishApproved(any(Payment.class));
        verify(paymentEventPublisherGateway, never()).publishPending(any(Payment.class));
    }

    @Test
    void shouldPersistPendingPaymentWhenExternalProcessorFails() {
        UUID orderId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("77.50");

        when(externalPaymentProcessorGateway.process(any(UUID.class), eq(clientId), eq(amount)))
                .thenThrow(new RuntimeException("Serviço indisponível"));

        Payment result = processPaymentUseCase.execute(orderId, clientId, amount);

        assertNotNull(result);
        assertEquals(PaymentStatus.PENDING, result.getStatus());

        var savedEntity = springDataPaymentRepository.findByOrderId(orderId);
        assertTrue(savedEntity.isPresent());
        assertEquals("PENDING", savedEntity.get().getStatus());
        assertEquals(new BigDecimal("77.50"), savedEntity.get().getAmount());
        assertEquals(clientId, savedEntity.get().getClientId());

        verify(paymentEventPublisherGateway, never()).publishApproved(any(Payment.class));
        verify(paymentEventPublisherGateway, times(1)).publishPending(any(Payment.class));
    }
}