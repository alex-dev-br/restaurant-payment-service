package br.com.fiap.restaurant.payment.infra.client.adapter;

import br.com.fiap.restaurant.payment.infra.client.processor.ExternalPaymentProcessorClient;
import br.com.fiap.restaurant.payment.support.AbstractMessagingIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "app.payment.retry.scheduler.enabled=false",
        "resilience4j.circuitbreaker.instances.externalPaymentProcessor.sliding-window-size=4",
        "resilience4j.circuitbreaker.instances.externalPaymentProcessor.minimum-number-of-calls=4",
        "resilience4j.circuitbreaker.instances.externalPaymentProcessor.failure-rate-threshold=50",
        "resilience4j.circuitbreaker.instances.externalPaymentProcessor.wait-duration-in-open-state=10s"
})
@ActiveProfiles("test")
class ResilientExternalPaymentProcessorCircuitBreakerIntegrationTest extends AbstractMessagingIntegrationTest {

    @Autowired
    private ResilientExternalPaymentProcessorGateway gateway;

    @MockitoBean
    private ExternalPaymentProcessorClient client;

    @Test
    void shouldOpenCircuitBreakerAfterRepeatedFailures() {
        AtomicInteger attempts = new AtomicInteger();

        when(client.process(any(UUID.class), any(UUID.class), any(BigDecimal.class)))
                .thenAnswer(invocation -> {
                    attempts.incrementAndGet();
                    throw new RuntimeException("processor down");
                });

        for (int i = 0; i < 6; i++) {
            gateway.process(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    new BigDecimal("100.00")
            );
        }

        int attemptsAfterFailures = attempts.get();

        boolean result = gateway.process(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100.00")
        );

        assertFalse(result);
        assertTrue(attempts.get() <= attemptsAfterFailures + 1);
    }
}
