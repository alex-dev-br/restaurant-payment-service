package br.com.fiap.restaurant.payment.infra.client.adapter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ResilientExternalPaymentProcessorGatewayTest {

    private ResilientExternalPaymentProcessorExecutor executor;
    private ResilientExternalPaymentProcessorGateway gateway;

    @BeforeEach
    void setUp() {
        executor = mock(ResilientExternalPaymentProcessorExecutor.class);
        gateway = new ResilientExternalPaymentProcessorGateway(executor);
    }

    @Test
    void shouldReturnTrueWhenExecutorSucceeds() {
        when(executor.processAsync(any(UUID.class), any(UUID.class), any(BigDecimal.class)))
                .thenReturn(CompletableFuture.completedFuture(true));

        boolean result = gateway.process(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100.00")
        );

        assertTrue(result);
    }

    @Test
    void shouldReturnFalseWhenExecutorFails() {
        CompletableFuture<Boolean> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("processor unavailable"));

        when(executor.processAsync(any(UUID.class), any(UUID.class), any(BigDecimal.class)))
                .thenReturn(failedFuture);

        boolean result = gateway.process(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100.00")
        );

        assertFalse(result);
    }
}
