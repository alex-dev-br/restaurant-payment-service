package br.com.fiap.restaurant.payment.infra.client.adapter;

import br.com.fiap.restaurant.payment.infra.client.processor.ExternalPaymentProcessorClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "app.payment.retry.scheduler.enabled=false",
        "resilience4j.bulkhead.instances.externalPaymentProcessor.max-concurrent-calls=1",
        "resilience4j.bulkhead.instances.externalPaymentProcessor.max-wait-duration=0",
        "resilience4j.retry.instances.externalPaymentProcessor.max-attempts=1",
        "resilience4j.timelimiter.instances.externalPaymentProcessor.timeout-duration=5s"
})
class ResilientExternalPaymentProcessorBulkheadIntegrationTest {

    @Autowired
    private ResilientExternalPaymentProcessorGateway gateway;

    @MockitoBean
    private ExternalPaymentProcessorClient client;

    @Test
    void shouldRejectSecondCallWhenBulkheadIsFull() throws Exception {
        when(client.process(any(UUID.class), any(UUID.class), any(BigDecimal.class)))
                .thenAnswer(invocation -> {
                    Thread.sleep(2000);
                    return true;
                });

        ExecutorService testExecutor = Executors.newFixedThreadPool(2);

        try {
            Future<Boolean> firstCall = testExecutor.submit(() ->
                    gateway.process(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("100.00"))
            );

            Thread.sleep(200);

            Future<Boolean> secondCall = testExecutor.submit(() ->
                    gateway.process(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("100.00"))
            );

            assertFalse(secondCall.get());
            assertTrue(firstCall.get());

        } finally {
            testExecutor.shutdownNow();
        }
    }
}
