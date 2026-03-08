package br.com.fiap.restaurant.payment.core.usecase;

import br.com.fiap.restaurant.payment.core.domain.gateway.ExternalPaymentProcessorGateway;
import br.com.fiap.restaurant.payment.core.domain.gateway.PaymentEventPublisherGateway;
import br.com.fiap.restaurant.payment.core.domain.gateway.PaymentObservabilityGateway;
import br.com.fiap.restaurant.payment.core.domain.gateway.PaymentRepositoryGateway;
import br.com.fiap.restaurant.payment.core.domain.model.Payment;
import br.com.fiap.restaurant.payment.core.domain.model.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProcessPaymentUseCaseTest {

    private PaymentRepositoryGateway paymentRepositoryGateway;
    private ExternalPaymentProcessorGateway externalPaymentProcessorGateway;
    private PaymentEventPublisherGateway paymentEventPublisherGateway;
    private PaymentObservabilityGateway paymentObservabilityGateway;
    private ProcessPaymentUseCase processPaymentUseCase;

    @BeforeEach
    void setUp() {
        paymentRepositoryGateway = mock(PaymentRepositoryGateway.class);
        externalPaymentProcessorGateway = mock(ExternalPaymentProcessorGateway.class);
        paymentEventPublisherGateway = mock(PaymentEventPublisherGateway.class);
        paymentObservabilityGateway = mock(PaymentObservabilityGateway.class);

        when(paymentObservabilityGateway.measureProcessing(any()))
                .thenAnswer(invocation -> {
                    Supplier<?> supplier = invocation.getArgument(0);
                    return supplier.get();
                });

        processPaymentUseCase = new ProcessPaymentUseCase(
                paymentRepositoryGateway,
                externalPaymentProcessorGateway,
                paymentEventPublisherGateway,
                paymentObservabilityGateway
        );
    }

    @Test
    void shouldProcessAndApprovePaymentSuccessfully() {
        UUID orderId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("100.00");

        when(paymentRepositoryGateway.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(paymentRepositoryGateway.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(externalPaymentProcessorGateway.process(any(UUID.class), eq(clientId), eq(amount)))
                .thenReturn(true);

        Payment result = processPaymentUseCase.execute(orderId, clientId, amount);

        assertNotNull(result);
        assertEquals(orderId, result.getOrderId());
        assertEquals(clientId, result.getClientId());
        assertEquals(PaymentStatus.APPROVED, result.getStatus());

        verify(paymentRepositoryGateway, times(2)).save(any(Payment.class));
        verify(externalPaymentProcessorGateway, times(1))
                .process(any(UUID.class), eq(clientId), eq(amount));
        verify(paymentEventPublisherGateway, times(1)).publishApproved(any(Payment.class));
        verify(paymentEventPublisherGateway, never()).publishPending(any(Payment.class));

        verify(paymentObservabilityGateway, times(1))
                .measureProcessing(any());
        verify(paymentObservabilityGateway, times(1))
                .logProcessingStarted(orderId, clientId, amount);
        verify(paymentObservabilityGateway, times(1))
                .logExternalProcessingStarted(any(Payment.class));
        verify(paymentObservabilityGateway, times(1))
                .logApproved(any(Payment.class));
        verify(paymentObservabilityGateway, never())
                .logPending(any(Payment.class));
        verify(paymentObservabilityGateway, never())
                .logExternalError(any(Payment.class), any(Exception.class));
        verify(paymentObservabilityGateway, never())
                .logIdempotentReuse(any(Payment.class));
        verify(paymentObservabilityGateway, never())
                .logConcurrentClaimReuse(any(Payment.class));
    }

    @Test
    void shouldMarkPaymentAsPendingWhenExternalProcessorReturnsFalse() {
        UUID orderId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("55.90");

        when(paymentRepositoryGateway.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(paymentRepositoryGateway.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(externalPaymentProcessorGateway.process(any(UUID.class), eq(clientId), eq(amount)))
                .thenReturn(false);

        Payment result = processPaymentUseCase.execute(orderId, clientId, amount);

        assertNotNull(result);
        assertEquals(orderId, result.getOrderId());
        assertEquals(clientId, result.getClientId());
        assertEquals(PaymentStatus.PENDING, result.getStatus());

        verify(paymentRepositoryGateway, times(2)).save(any(Payment.class));
        verify(externalPaymentProcessorGateway, times(1))
                .process(any(UUID.class), eq(clientId), eq(amount));
        verify(paymentEventPublisherGateway, never()).publishApproved(any(Payment.class));
        verify(paymentEventPublisherGateway, times(1)).publishPending(any(Payment.class));

        verify(paymentObservabilityGateway, times(1))
                .measureProcessing(any());
        verify(paymentObservabilityGateway, times(1))
                .logProcessingStarted(orderId, clientId, amount);
        verify(paymentObservabilityGateway, times(1))
                .logExternalProcessingStarted(any(Payment.class));
        verify(paymentObservabilityGateway, times(1))
                .logPending(any(Payment.class));
        verify(paymentObservabilityGateway, never())
                .logApproved(any(Payment.class));
        verify(paymentObservabilityGateway, never())
                .logExternalError(any(Payment.class), any(Exception.class));
        verify(paymentObservabilityGateway, never())
                .logIdempotentReuse(any(Payment.class));
        verify(paymentObservabilityGateway, never())
                .logConcurrentClaimReuse(any(Payment.class));
    }

    @Test
    void shouldMarkPaymentAsPendingWhenExternalProcessorThrowsException() {
        UUID orderId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("75.00");

        when(paymentRepositoryGateway.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(paymentRepositoryGateway.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(externalPaymentProcessorGateway.process(any(UUID.class), eq(clientId), eq(amount)))
                .thenThrow(new RuntimeException("Serviço indisponível"));

        Payment result = processPaymentUseCase.execute(orderId, clientId, amount);

        assertNotNull(result);
        assertEquals(orderId, result.getOrderId());
        assertEquals(clientId, result.getClientId());
        assertEquals(PaymentStatus.PENDING, result.getStatus());

        verify(paymentRepositoryGateway, times(2)).save(any(Payment.class));
        verify(externalPaymentProcessorGateway, times(1))
                .process(any(UUID.class), eq(clientId), eq(amount));
        verify(paymentEventPublisherGateway, never()).publishApproved(any(Payment.class));
        verify(paymentEventPublisherGateway, times(1)).publishPending(any(Payment.class));

        verify(paymentObservabilityGateway, times(1))
                .measureProcessing(any());
        verify(paymentObservabilityGateway, times(1))
                .logProcessingStarted(orderId, clientId, amount);
        verify(paymentObservabilityGateway, times(1))
                .logExternalProcessingStarted(any(Payment.class));
        verify(paymentObservabilityGateway, times(1))
                .logExternalError(any(Payment.class), any(Exception.class));
        verify(paymentObservabilityGateway, times(1))
                .logPending(any(Payment.class));
        verify(paymentObservabilityGateway, never())
                .logApproved(any(Payment.class));
        verify(paymentObservabilityGateway, never())
                .logIdempotentReuse(any(Payment.class));
        verify(paymentObservabilityGateway, never())
                .logConcurrentClaimReuse(any(Payment.class));
    }

    @Test
    void shouldReturnExistingPaymentWhenOrderAlreadyHasPayment() {
        UUID orderId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("42.00");
        Payment existingPayment = Payment.createPending(orderId, clientId, amount);

        when(paymentRepositoryGateway.findByOrderId(orderId)).thenReturn(Optional.of(existingPayment));

        Payment result = processPaymentUseCase.execute(orderId, clientId, amount);

        assertEquals(existingPayment, result);

        verify(externalPaymentProcessorGateway, never())
                .process(any(UUID.class), any(UUID.class), any(BigDecimal.class));
        verify(paymentRepositoryGateway, never()).save(any());
        verify(paymentEventPublisherGateway, never()).publishApproved(any());
        verify(paymentEventPublisherGateway, never()).publishPending(any());

        verify(paymentObservabilityGateway, times(1))
                .measureProcessing(any());
        verify(paymentObservabilityGateway, times(1))
                .logProcessingStarted(orderId, clientId, amount);
        verify(paymentObservabilityGateway, times(1))
                .logIdempotentReuse(existingPayment);
        verify(paymentObservabilityGateway, never())
                .logConcurrentClaimReuse(any(Payment.class));
        verify(paymentObservabilityGateway, never())
                .logExternalProcessingStarted(any(Payment.class));
        verify(paymentObservabilityGateway, never())
                .logApproved(any(Payment.class));
        verify(paymentObservabilityGateway, never())
                .logPending(any(Payment.class));
        verify(paymentObservabilityGateway, never())
                .logExternalError(any(Payment.class), any(Exception.class));
    }

    @Test
    void shouldReturnClaimedPaymentWithoutCallingExternalProcessorWhenAnotherFlowAlreadySavedIt() {
        UUID orderId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("50.00");

        Payment claimedByAnotherFlow = Payment.createPending(orderId, clientId, amount);

        when(paymentRepositoryGateway.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(paymentRepositoryGateway.save(any(Payment.class))).thenReturn(claimedByAnotherFlow);

        Payment result = processPaymentUseCase.execute(orderId, clientId, amount);

        assertNotNull(result);
        assertEquals(claimedByAnotherFlow.getId(), result.getId());
        assertEquals(orderId, result.getOrderId());
        assertEquals(clientId, result.getClientId());
        assertEquals(PaymentStatus.PENDING, result.getStatus());

        verify(paymentRepositoryGateway, times(1)).save(any(Payment.class));
        verify(externalPaymentProcessorGateway, never())
                .process(any(UUID.class), any(UUID.class), any(BigDecimal.class));
        verify(paymentEventPublisherGateway, never()).publishApproved(any());
        verify(paymentEventPublisherGateway, never()).publishPending(any());

        verify(paymentObservabilityGateway, times(1))
                .measureProcessing(any());
        verify(paymentObservabilityGateway, times(1))
                .logProcessingStarted(orderId, clientId, amount);
        verify(paymentObservabilityGateway, times(1))
                .logConcurrentClaimReuse(any(Payment.class));
        verify(paymentObservabilityGateway, never())
                .logIdempotentReuse(any(Payment.class));
        verify(paymentObservabilityGateway, never())
                .logExternalProcessingStarted(any(Payment.class));
        verify(paymentObservabilityGateway, never())
                .logApproved(any(Payment.class));
        verify(paymentObservabilityGateway, never())
                .logPending(any(Payment.class));
        verify(paymentObservabilityGateway, never())
                .logExternalError(any(Payment.class), any(Exception.class));
    }
}