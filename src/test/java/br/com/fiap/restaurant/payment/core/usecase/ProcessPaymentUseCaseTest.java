package br.com.fiap.restaurant.payment.core.usecase;

import br.com.fiap.restaurant.payment.core.domain.gateway.ExternalPaymentProcessorGateway;
import br.com.fiap.restaurant.payment.core.domain.gateway.PaymentEventPublisherGateway;
import br.com.fiap.restaurant.payment.core.domain.gateway.PaymentRepositoryGateway;
import br.com.fiap.restaurant.payment.core.domain.model.Payment;
import br.com.fiap.restaurant.payment.core.domain.model.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProcessPaymentUseCaseTest {

    private PaymentRepositoryGateway paymentRepositoryGateway;
    private ExternalPaymentProcessorGateway externalPaymentProcessorGateway;
    private PaymentEventPublisherGateway paymentEventPublisherGateway;
    private ProcessPaymentUseCase processPaymentUseCase;

    @BeforeEach
    void setUp() {
        paymentRepositoryGateway = mock(PaymentRepositoryGateway.class);
        externalPaymentProcessorGateway = mock(ExternalPaymentProcessorGateway.class);
        paymentEventPublisherGateway = mock(PaymentEventPublisherGateway.class);

        processPaymentUseCase = new ProcessPaymentUseCase(
                paymentRepositoryGateway,
                externalPaymentProcessorGateway,
                paymentEventPublisherGateway
        );
    }

    @Test
    void shouldProcessAndApprovePaymentSuccessfully() {
        UUID orderId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("100.00");

        when(paymentRepositoryGateway.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(externalPaymentProcessorGateway.process(orderId, amount)).thenReturn(true);
        when(paymentRepositoryGateway.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Payment result = processPaymentUseCase.execute(orderId, amount);

        assertNotNull(result);
        assertEquals(orderId, result.getOrderId());
        assertEquals(PaymentStatus.APPROVED, result.getStatus());

        verify(paymentEventPublisherGateway, times(1)).publishPaymentApproved(any(Payment.class));
        verify(paymentEventPublisherGateway, never()).publishPaymentPending(any(Payment.class));
    }

    @Test
    void shouldMarkPaymentAsPendingWhenExternalProcessorReturnsFalse() {
        UUID orderId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("55.90");

        when(paymentRepositoryGateway.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(externalPaymentProcessorGateway.process(orderId, amount)).thenReturn(false);
        when(paymentRepositoryGateway.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Payment result = processPaymentUseCase.execute(orderId, amount);

        assertNotNull(result);
        assertEquals(PaymentStatus.PENDING, result.getStatus());

        verify(paymentEventPublisherGateway, never()).publishPaymentApproved(any(Payment.class));
        verify(paymentEventPublisherGateway, times(1)).publishPaymentPending(any(Payment.class));
    }

    @Test
    void shouldMarkPaymentAsPendingWhenExternalProcessorThrowsException() {
        UUID orderId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("75.00");

        when(paymentRepositoryGateway.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(externalPaymentProcessorGateway.process(orderId, amount))
                .thenThrow(new RuntimeException("Serviço indisponível"));
        when(paymentRepositoryGateway.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Payment result = processPaymentUseCase.execute(orderId, amount);

        assertNotNull(result);
        assertEquals(PaymentStatus.PENDING, result.getStatus());

        verify(paymentEventPublisherGateway, never()).publishPaymentApproved(any(Payment.class));
        verify(paymentEventPublisherGateway, times(1)).publishPaymentPending(any(Payment.class));
    }

    @Test
    void shouldReturnExistingPaymentWhenOrderAlreadyHasPayment() {
        UUID orderId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("42.00");
        Payment existingPayment = Payment.createPending(orderId, amount);

        when(paymentRepositoryGateway.findByOrderId(orderId)).thenReturn(Optional.of(existingPayment));

        Payment result = processPaymentUseCase.execute(orderId, amount);

        assertEquals(existingPayment, result);

        verify(externalPaymentProcessorGateway, never()).process(any(), any());
        verify(paymentRepositoryGateway, never()).save(any());
        verify(paymentEventPublisherGateway, never()).publishPaymentApproved(any());
        verify(paymentEventPublisherGateway, never()).publishPaymentPending(any());
    }
}
