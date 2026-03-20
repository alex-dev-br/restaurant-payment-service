package br.com.fiap.restaurant.payment.core.usecase;

import br.com.fiap.restaurant.payment.core.domain.model.Payment;
import br.com.fiap.restaurant.payment.core.domain.model.PaymentStatus;
import br.com.fiap.restaurant.payment.core.gateway.ExternalPaymentProcessorGateway;
import br.com.fiap.restaurant.payment.core.gateway.PaymentFinalizationGateway;
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
    private PaymentFinalizationGateway paymentFinalizationGateway;
    private PaymentObservabilityGateway paymentObservabilityGateway;
    private RetryPendingPaymentsUseCase retryPendingPaymentsUseCase;

    @BeforeEach
    void setUp() {
        paymentRepositoryGateway = mock(PaymentRepositoryGateway.class);
        externalPaymentProcessorGateway = mock(ExternalPaymentProcessorGateway.class);
        paymentFinalizationGateway = mock(PaymentFinalizationGateway.class);
        paymentObservabilityGateway = mock(PaymentObservabilityGateway.class);

        retryPendingPaymentsUseCase = new RetryPendingPaymentsUseCase(
                paymentRepositoryGateway,
                externalPaymentProcessorGateway,
                paymentFinalizationGateway,
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

        when(paymentFinalizationGateway.saveApprovedAndEnqueue(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        retryPendingPaymentsUseCase.execute();

        verify(paymentRepositoryGateway).findRetryablePendingPayments(any(OffsetDateTime.class), eq(3));
        verify(paymentObservabilityGateway).logExternalProcessingStarted(pendingPayment);
        verify(externalPaymentProcessorGateway).process(
                pendingPayment.getId(),
                pendingPayment.getClientId(),
                pendingPayment.getAmount()
        );
        verify(paymentFinalizationGateway).saveApprovedAndEnqueue(any(Payment.class));
        verify(paymentFinalizationGateway, never()).savePendingAndEnqueue(any(Payment.class));
        verify(paymentFinalizationGateway, never()).saveFailedAndEnqueue(any(Payment.class));
        verify(paymentRepositoryGateway, never()).save(any(Payment.class));
        verify(paymentObservabilityGateway).logApproved(any(Payment.class));
        verify(paymentObservabilityGateway, never()).logPending(any(Payment.class));
        verify(paymentObservabilityGateway, never()).logFailed(any(Payment.class));
        verify(paymentObservabilityGateway, never()).logExternalError(any(Payment.class), any(Exception.class));
    }

    @Test
    void shouldKeepPaymentPendingAndPersistOnlyStateWhenRetryFailsAndPolicyDisablesPendingEvent() {
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
        verify(paymentFinalizationGateway, never()).saveApprovedAndEnqueue(any(Payment.class));
        verify(paymentFinalizationGateway, never()).savePendingAndEnqueue(any(Payment.class));
        verify(paymentFinalizationGateway, never()).saveFailedAndEnqueue(any(Payment.class));
        verify(paymentObservabilityGateway).logPending(any(Payment.class));
        verify(paymentObservabilityGateway, never()).logApproved(any(Payment.class));
        verify(paymentObservabilityGateway, never()).logFailed(any(Payment.class));

        Payment savedPayment = paymentCaptor.getValue();
        assertEquals(PaymentStatus.PENDING, savedPayment.getStatus());
        assertEquals(1, savedPayment.getRetryCount());
        assertNotNull(savedPayment.getLastRetryAt());
        assertNotNull(savedPayment.getNextRetryAt());
    }

    @Test
    void shouldEnqueuePendingEventWhenRetryFailsAndPolicyEnablesPendingEvent() {
        Payment pendingPayment = buildPendingPayment();

        RetryPendingPaymentsUseCase useCase = new RetryPendingPaymentsUseCase(
                paymentRepositoryGateway,
                externalPaymentProcessorGateway,
                paymentFinalizationGateway,
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

        when(paymentFinalizationGateway.savePendingAndEnqueue(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute();

        verify(paymentFinalizationGateway).savePendingAndEnqueue(any(Payment.class));
        verify(paymentRepositoryGateway, never()).save(any(Payment.class));
        verify(paymentFinalizationGateway, never()).saveApprovedAndEnqueue(any(Payment.class));
        verify(paymentFinalizationGateway, never()).saveFailedAndEnqueue(any(Payment.class));
        verify(paymentObservabilityGateway).logPending(any(Payment.class));
    }

    @Test
    void shouldKeepPaymentPendingAndPersistOnlyStateWhenRetryThrowsExceptionAndPolicyDisablesPendingEvent() {
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
        verify(paymentRepositoryGateway).save(any(Payment.class));
        verify(paymentFinalizationGateway, never()).saveApprovedAndEnqueue(any(Payment.class));
        verify(paymentFinalizationGateway, never()).savePendingAndEnqueue(any(Payment.class));
        verify(paymentFinalizationGateway, never()).saveFailedAndEnqueue(any(Payment.class));
        verify(paymentObservabilityGateway).logPending(any(Payment.class));
        verify(paymentObservabilityGateway, never()).logApproved(any(Payment.class));
        verify(paymentObservabilityGateway, never()).logFailed(any(Payment.class));
    }

    @Test
    void shouldMarkPaymentAsFailedWhenMaxAttemptsAreExhausted() {
        Payment pendingPayment = new Payment(
                UUID.randomUUID(),
                100L,
                UUID.randomUUID(),
                new BigDecimal("150.00"),
                PaymentStatus.PENDING,
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                2,
                OffsetDateTime.now().minusSeconds(30),
                OffsetDateTime.now().minusSeconds(1)
        );

        when(paymentRepositoryGateway.findRetryablePendingPayments(any(OffsetDateTime.class), eq(3)))
                .thenReturn(List.of(pendingPayment));

        when(externalPaymentProcessorGateway.process(
                pendingPayment.getId(),
                pendingPayment.getClientId(),
                pendingPayment.getAmount()
        )).thenReturn(false);

        when(paymentFinalizationGateway.saveFailedAndEnqueue(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        retryPendingPaymentsUseCase.execute();

        verify(paymentFinalizationGateway).saveFailedAndEnqueue(argThat(payment ->
                payment.getStatus() == PaymentStatus.FAILED
                        && payment.getRetryCount() == 3
                        && payment.getNextRetryAt() == null
        ));

        verify(paymentFinalizationGateway, never()).saveApprovedAndEnqueue(any(Payment.class));
        verify(paymentFinalizationGateway, never()).savePendingAndEnqueue(any(Payment.class));
        verify(paymentRepositoryGateway, never()).save(any(Payment.class));
        verify(paymentObservabilityGateway).logFailed(any(Payment.class));
        verify(paymentObservabilityGateway, never()).logApproved(any(Payment.class));
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
