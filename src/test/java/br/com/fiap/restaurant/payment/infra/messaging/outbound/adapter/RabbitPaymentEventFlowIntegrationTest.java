package br.com.fiap.restaurant.payment.infra.messaging.outbound.adapter;

import br.com.fiap.restaurant.payment.core.gateway.ExternalPaymentProcessorGateway;
import br.com.fiap.restaurant.payment.core.usecase.ProcessPaymentUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "spring.rabbitmq.listener.simple.auto-startup=false"
})
@ActiveProfiles("rabbit")
class RabbitPaymentEventFlowIntegrationTest {

    @Autowired
    private ProcessPaymentUseCase processPaymentUseCase;

    @MockitoBean
    private ExternalPaymentProcessorGateway externalPaymentProcessorGateway;

    @Test
    void shouldPublishApprovedEventToRabbit() {

        UUID orderId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("120.00");

        when(externalPaymentProcessorGateway.process(
                any(UUID.class),
                any(UUID.class),
                any(BigDecimal.class)
        )).thenReturn(true);

        processPaymentUseCase.execute(orderId, clientId, amount);
    }

    @Test
    void shouldPublishPendingEventWhenProcessorFails() {

        UUID orderId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("150.00");

        when(externalPaymentProcessorGateway.process(
                any(UUID.class),
                any(UUID.class),
                any(BigDecimal.class)
        )).thenReturn(false);

        processPaymentUseCase.execute(orderId, clientId, amount);
    }

    @Test
    void shouldPublishPendingEventWhenProcessorThrowsException() {

        UUID orderId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("180.00");

        doThrow(new RuntimeException("processor unavailable"))
                .when(externalPaymentProcessorGateway)
                .process(
                        any(UUID.class),
                        any(UUID.class),
                        any(BigDecimal.class)
                );

        processPaymentUseCase.execute(orderId, clientId, amount);
    }
}
