package br.com.fiap.restaurant.payment.core.usecase;

import br.com.fiap.restaurant.payment.core.domain.model.Payment;
import br.com.fiap.restaurant.payment.core.domain.model.PaymentStatus;
import br.com.fiap.restaurant.payment.core.gateway.ExternalPaymentProcessorGateway;
import br.com.fiap.restaurant.payment.core.gateway.PaymentEventPublisherGateway;
import br.com.fiap.restaurant.payment.core.gateway.PaymentObservabilityGateway;
import br.com.fiap.restaurant.payment.core.gateway.PaymentRepositoryGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RetryPendingPaymentsUseCaseTest {

    private PaymentRepositoryGateway paymentRepositoryGateway;
    private ExternalPaymentProcessorGateway externalPaymentProcessorGateway;
    private PaymentEventPublisherGateway paymentEventPublisherGateway;
    private PaymentObservabilityGateway paymentObservabilityGateway;
    private RetryPendingPaymentsUseCase retryPendingPaymentsUseCase;

    @BeforeEach
    void setUp() {
        paymentRepositoryGateway = mock(PaymentRepositoryGateway.class);
        externalPaymentProcessorGateway = mock(ExternalPaymentProcessorGateway.class);
        paymentEventPublisherGateway = mock(PaymentEventPublisherGateway.class);
        paymentObservabilityGateway = mock(PaymentObservabilityGateway.class);

        retryPendingPaymentsUseCase = new RetryPendingPaymentsUseCase(
                paymentRepositoryGateway,
                externalPaymentProcessorGateway,
                paymentEventPublisherGateway,
                paymentObservabilityGateway,
                Duration.ofSeconds(30),
                3,
                false
        );
    }

    @Test
    void shouldApprovePendingPaymentsWhenRetrySucceeds() {
        Payment pendingPayment = buildPendingPayment();

        when(paymentRepositoryGateway.findRetryablePendingPayments(any(OffsetDateTime.class), eq(3)))
                .thenReturn(List.of(pendingPayment));

        when(externalPaymentProcessorGateway.process(
                pendingPayment.getId(),
                pendingPayment.getClientId(),
                pendingPayment.getAmount()
        )).thenReturn(true);

        when(paymentRepositoryGateway.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        retryPendingPaymentsUseCase.execute();

        verify(paymentRepositoryGateway).findRetryablePendingPayments(any(OffsetDateTime.class), eq(3));
        verify(paymentObservabilityGateway).logExternalProcessingStarted(pendingPayment);
        verify(externalPaymentProcessorGateway).process(
                pendingPayment.getId(),
                pendingPayment.getClientId(),
                pendingPayment.getAmount()
        );
        verify(paymentRepositoryGateway).save(any(Payment.class));
        verify(paymentEventPublisherGateway).publishApproved(any(Payment.class));
        verify(paymentEventPublisherGateway, never()).publishPending(any(Payment.class));
        verify(paymentObservabilityGateway).logApproved(any(Payment.class));
        verify(paymentObservabilityGateway, never()).logPending(any(Payment.class));
        verify(paymentObservabilityGateway, never()).logExternalError(any(Payment.class), any(Exception.class));
    }

    @Test
    void shouldKeepPaymentPendingAndNotPublishPendingWhenRetryFailsAndPolicyDisablesIt() {
        Payment pendingPayment = buildPendingPayment();

        when(paymentRepositoryGateway.findRetryablePendingPayments(any(OffsetDateTime.class), eq(3)))
                .thenReturn(List.of(pendingPayment));

        when(externalPaymentProcessorGateway.process(
                pendingPayment.getId(),
                pendingPayment.getClientId(),
                pendingPayment.getAmount()
        )).thenReturn(false);

        when(paymentRepositoryGateway.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        retryPendingPaymentsUseCase.execute();

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);

        verify(paymentRepositoryGateway).findRetryablePendingPayments(any(OffsetDateTime.class), eq(3));
        verify(paymentRepositoryGateway).save(paymentCaptor.capture());
        verify(paymentEventPublisherGateway, never()).publishApproved(any(Payment.class));
        verify(paymentEventPublisherGateway, never()).publishPending(any(Payment.class));
        verify(paymentObservabilityGateway).logPending(any(Payment.class));

        Payment savedPayment = paymentCaptor.getValue();
        assertEquals(PaymentStatus.PENDING, savedPayment.getStatus());
        assertEquals(1, savedPayment.getRetryCount());
        assertNotNull(savedPayment.getLastRetryAt());
        assertNotNull(savedPayment.getNextRetryAt());
    }

    @Test
    void shouldPublishPendingWhenRetryFailsAndPolicyEnablesIt() {
        Payment pendingPayment = buildPendingPayment();

        RetryPendingPaymentsUseCase useCase = new RetryPendingPaymentsUseCase(
                paymentRepositoryGateway,
                externalPaymentProcessorGateway,
                paymentEventPublisherGateway,
                paymentObservabilityGateway,
                Duration.ofSeconds(30),
                3,
                true
        );

        when(paymentRepositoryGateway.findRetryablePendingPayments(any(OffsetDateTime.class), eq(3)))
                .thenReturn(List.of(pendingPayment));

        when(externalPaymentProcessorGateway.process(
                pendingPayment.getId(),
                pendingPayment.getClientId(),
                pendingPayment.getAmount()
        )).thenReturn(false);

        when(paymentRepositoryGateway.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute();

        verify(paymentEventPublisherGateway).publishPending(any(Payment.class));
    }

    @Test
    void shouldKeepPaymentPendingAndNotPublishPendingWhenRetryThrowsExceptionAndPolicyDisablesIt() {
        Payment pendingPayment = buildPendingPayment();

        when(paymentRepositoryGateway.findRetryablePendingPayments(any(OffsetDateTime.class), eq(3)))
                .thenReturn(List.of(pendingPayment));

        when(externalPaymentProcessorGateway.process(
                pendingPayment.getId(),
                pendingPayment.getClientId(),
                pendingPayment.getAmount()
        )).thenThrow(new RuntimeException("processor unavailable"));

        when(paymentRepositoryGateway.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        retryPendingPaymentsUseCase.execute();

        verify(paymentObservabilityGateway).logExternalError(
                eq(pendingPayment),
                any(RuntimeException.class)
        );
        verify(paymentEventPublisherGateway, never()).publishApproved(any(Payment.class));
        verify(paymentEventPublisherGateway, never()).publishPending(any(Payment.class));
        verify(paymentObservabilityGateway).logPending(any(Payment.class));
    }

    private Payment buildPendingPayment() {
        return new Payment(
                UUID.randomUUID(),
                100L,
                UUID.randomUUID(),
                new BigDecimal("150.00"),
                PaymentStatus.PENDING,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
    }
}
