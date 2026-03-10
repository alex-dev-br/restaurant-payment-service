package br.com.fiap.restaurant.payment.infra.client.adapter;

import br.com.fiap.restaurant.payment.infra.client.processor.ExternalPaymentProcessorClient;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ResilientExternalPaymentProcessorGatewayTest {

    private ExternalPaymentProcessorClient client;
    private ExecutorService executorService;
    private RetryRegistry retryRegistry;
    private CircuitBreakerRegistry circuitBreakerRegistry;
    private TimeLimiterRegistry timeLimiterRegistry;
    private ResilientExternalPaymentProcessorGateway gateway;

    @BeforeEach
    void setUp() {
        client = mock(ExternalPaymentProcessorClient.class);
        executorService = Executors.newSingleThreadExecutor();

        retryRegistry = RetryRegistry.ofDefaults();
        circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        timeLimiterRegistry = TimeLimiterRegistry.ofDefaults();

        retryRegistry.retry("externalPaymentProcessor");
        circuitBreakerRegistry.circuitBreaker("externalPaymentProcessor");
        timeLimiterRegistry.timeLimiter(
                "externalPaymentProcessor",
                io.github.resilience4j.timelimiter.TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofSeconds(2))
                        .build()
        );

        gateway = new ResilientExternalPaymentProcessorGateway(
                client,
                executorService,
                retryRegistry,
                circuitBreakerRegistry,
                timeLimiterRegistry
        );
    }

    @AfterEach
    void tearDown() {
        executorService.shutdownNow();
    }

    @Test
    void shouldReturnTrueWhenClientSucceeds() {
        when(client.process(any(UUID.class), any(UUID.class), any(BigDecimal.class)))
                .thenReturn(true);

        boolean result = gateway.process(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100.00")
        );

        assertTrue(result);
    }

    @Test
    void shouldReturnFalseWhenClientThrowsException() {
        when(client.process(any(UUID.class), any(UUID.class), any(BigDecimal.class)))
                .thenThrow(new RuntimeException("processor unavailable"));

        boolean result = gateway.process(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100.00")
        );

        assertFalse(result);
    }
}