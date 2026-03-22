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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "app.payment.retry.scheduler.enabled=false"
})
@ActiveProfiles("test")
class ResilientExternalPaymentProcessorRetryIntegrationTest extends AbstractMessagingIntegrationTest {

    @Autowired
    private ResilientExternalPaymentProcessorGateway gateway;

    @MockitoBean
    private ExternalPaymentProcessorClient client;

    @Test
    void shouldRetryWhenProcessorFails() {
        AtomicInteger attempts = new AtomicInteger();

        when(client.process(any(UUID.class), any(UUID.class), any(BigDecimal.class)))
                .thenAnswer(invocation -> {
                    int currentAttempt = attempts.incrementAndGet();

                    if (currentAttempt < 3) {
                        throw new RuntimeException("temporary failure");
                    }

                    return true;
                });

        boolean result = gateway.process(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100.00")
        );

        assertTrue(result);
        assertEquals(3, attempts.get());
    }
}
