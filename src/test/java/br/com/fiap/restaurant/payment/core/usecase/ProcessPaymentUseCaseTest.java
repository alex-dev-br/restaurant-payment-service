package br.com.fiap.restaurant.payment.core.usecase;

import br.com.fiap.restaurant.payment.core.domain.model.Payment;
import br.com.fiap.restaurant.payment.core.domain.model.PaymentStatus;
import br.com.fiap.restaurant.payment.core.gateway.ExternalPaymentProcessorGateway;
import br.com.fiap.restaurant.payment.core.gateway.PaymentFinalizationGateway;
import br.com.fiap.restaurant.payment.core.gateway.PaymentObservabilityGateway;
import br.com.fiap.restaurant.payment.core.gateway.PaymentRepositoryGateway;
import br.com.fiap.restaurant.payment.core.usecase.command.ProcessPaymentCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProcessPaymentUseCaseTest {

    private PaymentRepositoryGateway paymentRepositoryGateway;
    private ExternalPaymentProcessorGateway externalPaymentProcessorGateway;
    private PaymentFinalizationGateway paymentFinalizationGateway;
    private PaymentObservabilityGateway paymentObservabilityGateway;
    private ProcessPaymentUseCase processPaymentUseCase;

    @BeforeEach
    void setUp() {
        paymentRepositoryGateway = mock(PaymentRepositoryGateway.class);
        externalPaymentProcessorGateway = mock(ExternalPaymentProcessorGateway.class);
        paymentFinalizationGateway = mock(PaymentFinalizationGateway.class);
        paymentObservabilityGateway = mock(PaymentObservabilityGateway.class);

        when(paymentObservabilityGateway.measureProcessing(any()))
                .thenAnswer(invocation -> {
                    Supplier<?> supplier = invocation.getArgument(0);
                    return supplier.get();
                });

        processPaymentUseCase = new ProcessPaymentUseCase(
                paymentRepositoryGateway,
                externalPaymentProcessorGateway,
                paymentFinalizationGateway,
                paymentObservabilityGateway,
                Duration.ofSeconds(30)
        );
    }

    @Test
    void shouldProcessAndApprovePaymentSuccessfully() {
        Long orderId = 1L;
        UUID clientId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("89.90");

        when(paymentRepositoryGateway.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(paymentRepositoryGateway.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(externalPaymentProcessorGateway.process(any(UUID.class), eq(clientId), eq(amount)))
                .thenReturn(true);
        when(paymentFinalizationGateway.saveApprovedAndEnqueue(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProcessPaymentCommand command = new ProcessPaymentCommand(orderId, clientId, amount);
        Payment result = processPaymentUseCase.execute(command);

        assertNotNull(result);
        assertEquals(orderId, result.getOrderId());
        assertEquals(clientId, result.getClientId());
        assertEquals(PaymentStatus.APPROVED, result.getStatus());
        assertEquals(0, result.getRetryCount());
        assertNull(result.getLastRetryAt());
        assertNull(result.getNextRetryAt());

        verify(paymentRepositoryGateway, times(1)).save(any(Payment.class));
        verify(externalPaymentProcessorGateway, times(1))
                .process(any(UUID.class), eq(clientId), eq(amount));
        verify(paymentFinalizationGateway, times(1))
                .saveApprovedAndEnqueue(any(Payment.class));
        verify(paymentFinalizationGateway, never())
                .savePendingAndEnqueue(any(Payment.class));
        verify(paymentFinalizationGateway, never())
                .saveFailedAndEnqueue(any(Payment.class));

        verify(paymentObservabilityGateway, times(1)).measureProcessing(any());
        verify(paymentObservabilityGateway, times(1)).logProcessingStarted(orderId, clientId, amount);
        verify(paymentObservabilityGateway, times(1)).logExternalProcessingStarted(any(Payment.class));
        verify(paymentObservabilityGateway, times(1)).logApproved(any(Payment.class));
        verify(paymentObservabilityGateway, never()).logPending(any(Payment.class));
        verify(paymentObservabilityGateway, never()).logFailed(any(Payment.class));
        verify(paymentObservabilityGateway, never()).logExternalError(any(Payment.class), any(Exception.class));
        verify(paymentObservabilityGateway, never()).logIdempotentReuse(any(Payment.class));
        verify(paymentObservabilityGateway, never()).logConcurrentClaimReuse(any(Payment.class));
    }

    @Test
    void shouldRegisterInitialRetryMetadataWhenExternalProcessorReturnsFalse() {
        Long orderId = 1L;
        UUID clientId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("55.90");

        when(paymentRepositoryGateway.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(paymentRepositoryGateway.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(externalPaymentProcessorGateway.process(any(UUID.class), eq(clientId), eq(amount)))
                .thenReturn(false);
        when(paymentFinalizationGateway.savePendingAndEnqueue(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProcessPaymentCommand command = new ProcessPaymentCommand(orderId, clientId, amount);
        Payment result = processPaymentUseCase.execute(command);

        assertNotNull(result);
        assertEquals(orderId, result.getOrderId());
        assertEquals(clientId, result.getClientId());
        assertEquals(PaymentStatus.PENDING, result.getStatus());
        assertEquals(1, result.getRetryCount());
        assertNotNull(result.getLastRetryAt());
        assertNotNull(result.getNextRetryAt());

        verify(paymentRepositoryGateway, times(1)).save(any(Payment.class));
        verify(externalPaymentProcessorGateway, times(1))
                .process(any(UUID.class), eq(clientId), eq(amount));
        verify(paymentFinalizationGateway, never())
                .saveApprovedAndEnqueue(any(Payment.class));
        verify(paymentFinalizationGateway, times(1))
                .savePendingAndEnqueue(any(Payment.class));
        verify(paymentFinalizationGateway, never())
                .saveFailedAndEnqueue(any(Payment.class));

        verify(paymentObservabilityGateway, times(1)).measureProcessing(any());
        verify(paymentObservabilityGateway, times(1)).logProcessingStarted(orderId, clientId, amount);
        verify(paymentObservabilityGateway, times(1)).logExternalProcessingStarted(any(Payment.class));
        verify(paymentObservabilityGateway, times(1)).logPending(any(Payment.class));
        verify(paymentObservabilityGateway, never()).logApproved(any(Payment.class));
        verify(paymentObservabilityGateway, never()).logFailed(any(Payment.class));
        verify(paymentObservabilityGateway, never()).logExternalError(any(Payment.class), any(Exception.class));
        verify(paymentObservabilityGateway, never()).logIdempotentReuse(any(Payment.class));
        verify(paymentObservabilityGateway, never()).logConcurrentClaimReuse(any(Payment.class));
    }

    @Test
    void shouldRegisterInitialRetryMetadataWhenExternalProcessorThrowsException() {
        Long orderId = 1L;
        UUID clientId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("75.00");

        when(paymentRepositoryGateway.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(paymentRepositoryGateway.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(externalPaymentProcessorGateway.process(any(UUID.class), eq(clientId), eq(amount)))
                .thenThrow(new RuntimeException("Serviço indisponível"));
        when(paymentFinalizationGateway.savePendingAndEnqueue(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProcessPaymentCommand command = new ProcessPaymentCommand(orderId, clientId, amount);
        Payment result = processPaymentUseCase.execute(command);

        assertNotNull(result);
        assertEquals(orderId, result.getOrderId());
        assertEquals(clientId, result.getClientId());
        assertEquals(PaymentStatus.PENDING, result.getStatus());
        assertEquals(1, result.getRetryCount());
        assertNotNull(result.getLastRetryAt());
        assertNotNull(result.getNextRetryAt());

        verify(paymentRepositoryGateway, times(1)).save(any(Payment.class));
        verify(externalPaymentProcessorGateway, times(1))
                .process(any(UUID.class), eq(clientId), eq(amount));
        verify(paymentFinalizationGateway, never())
                .saveApprovedAndEnqueue(any(Payment.class));
        verify(paymentFinalizationGateway, times(1))
                .savePendingAndEnqueue(any(Payment.class));
        verify(paymentFinalizationGateway, never())
                .saveFailedAndEnqueue(any(Payment.class));

        verify(paymentObservabilityGateway, times(1)).measureProcessing(any());
        verify(paymentObservabilityGateway, times(1)).logProcessingStarted(orderId, clientId, amount);
        verify(paymentObservabilityGateway, times(1)).logExternalProcessingStarted(any(Payment.class));
        verify(paymentObservabilityGateway, times(1)).logPending(any(Payment.class));
        verify(paymentObservabilityGateway, times(1)).logExternalError(any(Payment.class), any(Exception.class));
        verify(paymentObservabilityGateway, never()).logApproved(any(Payment.class));
        verify(paymentObservabilityGateway, never()).logFailed(any(Payment.class));
        verify(paymentObservabilityGateway, never()).logIdempotentReuse(any(Payment.class));
        verify(paymentObservabilityGateway, never()).logConcurrentClaimReuse(any(Payment.class));
    }

    @Test
    void shouldReturnExistingPaymentWhenPaymentAlreadyExistsForOrder() {
        Long orderId = 4L;
        UUID clientId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("120.00");

        Payment existingPayment = Payment.createPending(orderId, clientId, amount);

        when(paymentRepositoryGateway.findByOrderId(orderId)).thenReturn(Optional.of(existingPayment));

        ProcessPaymentCommand command = new ProcessPaymentCommand(orderId, clientId, amount);
        Payment result = processPaymentUseCase.execute(command);

        assertNotNull(result);
        assertEquals(existingPayment.getId(), result.getId());

        verify(paymentRepositoryGateway, never()).save(any(Payment.class));
        verify(externalPaymentProcessorGateway, never())
                .process(any(UUID.class), any(UUID.class), any(BigDecimal.class));
        verify(paymentFinalizationGateway, never()).saveApprovedAndEnqueue(any(Payment.class));
        verify(paymentFinalizationGateway, never()).savePendingAndEnqueue(any(Payment.class));
        verify(paymentFinalizationGateway, never()).saveFailedAndEnqueue(any(Payment.class));

        verify(paymentObservabilityGateway, times(1)).logIdempotentReuse(existingPayment);
    }

    @Test
    void shouldReturnClaimedPaymentWhenAnotherFlowAlreadyPersistedIt() {
        Long orderId = 5L;
        UUID clientId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("49.90");

        Payment claimedByAnotherFlow = Payment.createPending(orderId, clientId, amount);

        when(paymentRepositoryGateway.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(paymentRepositoryGateway.save(any(Payment.class))).thenReturn(claimedByAnotherFlow);

        ProcessPaymentCommand command = new ProcessPaymentCommand(orderId, clientId, amount);
        Payment result = processPaymentUseCase.execute(command);

        assertNotNull(result);
        assertEquals(claimedByAnotherFlow.getId(), result.getId());
        assertEquals(PaymentStatus.PENDING, result.getStatus());

        verify(paymentRepositoryGateway, times(1)).save(any(Payment.class));
        verify(externalPaymentProcessorGateway, never())
                .process(any(UUID.class), any(UUID.class), any(BigDecimal.class));
        verify(paymentFinalizationGateway, never()).saveApprovedAndEnqueue(any(Payment.class));
        verify(paymentFinalizationGateway, never()).savePendingAndEnqueue(any(Payment.class));
        verify(paymentFinalizationGateway, never()).saveFailedAndEnqueue(any(Payment.class));

        verify(paymentObservabilityGateway, times(1)).measureProcessing(any());
        verify(paymentObservabilityGateway, times(1)).logProcessingStarted(orderId, clientId, amount);
        verify(paymentObservabilityGateway, times(1)).logConcurrentClaimReuse(any(Payment.class));
        verify(paymentObservabilityGateway, never()).logIdempotentReuse(any(Payment.class));
        verify(paymentObservabilityGateway, never()).logExternalProcessingStarted(any(Payment.class));
        verify(paymentObservabilityGateway, never()).logApproved(any(Payment.class));
        verify(paymentObservabilityGateway, never()).logPending(any(Payment.class));
        verify(paymentObservabilityGateway, never()).logFailed(any(Payment.class));
        verify(paymentObservabilityGateway, never()).logExternalError(any(Payment.class), any(Exception.class));
    }
}
