package br.com.fiap.restaurant.payment.core.usecase;

import br.com.fiap.restaurant.payment.core.domain.model.Payment;
import br.com.fiap.restaurant.payment.core.domain.model.PaymentStatus;
import br.com.fiap.restaurant.payment.core.gateway.ExternalPaymentProcessorGateway;
import br.com.fiap.restaurant.payment.core.gateway.PaymentEventPublisherGateway;
import br.com.fiap.restaurant.payment.core.gateway.PaymentObservabilityGateway;
import br.com.fiap.restaurant.payment.core.gateway.PaymentRepositoryGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

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
                paymentObservabilityGateway
        );
    }

    @Test
    void shouldApprovePendingPaymentsWhenRetrySucceeds() {
        Payment pendingPayment = buildPendingPayment();

        when(paymentRepositoryGateway.findByStatus(PaymentStatus.PENDING))
                .thenReturn(List.of(pendingPayment));

        when(externalPaymentProcessorGateway.process(
                pendingPayment.getId(),
                pendingPayment.getClientId(),
                pendingPayment.getAmount()
        )).thenReturn(true);

        when(paymentRepositoryGateway.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        retryPendingPaymentsUseCase.execute();

        verify(paymentRepositoryGateway).findByStatus(PaymentStatus.PENDING);
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
    void shouldKeepPaymentPendingWhenRetryFails() {
        Payment pendingPayment = buildPendingPayment();

        when(paymentRepositoryGateway.findByStatus(PaymentStatus.PENDING))
                .thenReturn(List.of(pendingPayment));

        when(externalPaymentProcessorGateway.process(
                pendingPayment.getId(),
                pendingPayment.getClientId(),
                pendingPayment.getAmount()
        )).thenReturn(false);

        when(paymentRepositoryGateway.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        retryPendingPaymentsUseCase.execute();

        verify(paymentRepositoryGateway).findByStatus(PaymentStatus.PENDING);
        verify(paymentObservabilityGateway).logExternalProcessingStarted(pendingPayment);
        verify(externalPaymentProcessorGateway).process(
                pendingPayment.getId(),
                pendingPayment.getClientId(),
                pendingPayment.getAmount()
        );
        verify(paymentRepositoryGateway).save(any(Payment.class));
        verify(paymentEventPublisherGateway).publishPending(any(Payment.class));
        verify(paymentEventPublisherGateway, never()).publishApproved(any(Payment.class));
        verify(paymentObservabilityGateway).logPending(any(Payment.class));
        verify(paymentObservabilityGateway, never()).logApproved(any(Payment.class));
        verify(paymentObservabilityGateway, never()).logExternalError(any(Payment.class), any(Exception.class));
    }

    @Test
    void shouldKeepPaymentPendingWhenRetryThrowsException() {
        Payment pendingPayment = buildPendingPayment();

        when(paymentRepositoryGateway.findByStatus(PaymentStatus.PENDING))
                .thenReturn(List.of(pendingPayment));

        when(externalPaymentProcessorGateway.process(
                pendingPayment.getId(),
                pendingPayment.getClientId(),
                pendingPayment.getAmount()
        )).thenThrow(new RuntimeException("processor unavailable"));

        when(paymentRepositoryGateway.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        retryPendingPaymentsUseCase.execute();

        verify(paymentRepositoryGateway).findByStatus(PaymentStatus.PENDING);
        verify(paymentObservabilityGateway).logExternalProcessingStarted(pendingPayment);
        verify(externalPaymentProcessorGateway).process(
                pendingPayment.getId(),
                pendingPayment.getClientId(),
                pendingPayment.getAmount()
        );
        verify(paymentObservabilityGateway).logExternalError(
                eq(pendingPayment),
                any(RuntimeException.class)
        );
        verify(paymentRepositoryGateway).save(any(Payment.class));
        verify(paymentEventPublisherGateway).publishPending(any(Payment.class));
        verify(paymentEventPublisherGateway, never()).publishApproved(any(Payment.class));
        verify(paymentObservabilityGateway).logPending(any(Payment.class));
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