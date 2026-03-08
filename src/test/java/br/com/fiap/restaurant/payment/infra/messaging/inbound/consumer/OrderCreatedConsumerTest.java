package br.com.fiap.restaurant.payment.infra.messaging.inbound.consumer;

import br.com.fiap.restaurant.payment.core.usecase.ProcessPaymentUseCase;
import br.com.fiap.restaurant.payment.infra.messaging.inbound.dto.OrderCreatedMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.*;

class OrderCreatedConsumerTest {

    private ProcessPaymentUseCase processPaymentUseCase;
    private OrderCreatedConsumer orderCreatedConsumer;

    @BeforeEach
    void setUp() {
        processPaymentUseCase = mock(ProcessPaymentUseCase.class);
        orderCreatedConsumer = new OrderCreatedConsumer(processPaymentUseCase);
    }

    @Test
    void shouldCallProcessPaymentUseCaseWhenOrderCreatedMessageIsConsumed() {
        UUID orderId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("120.00");

        OrderCreatedMessage message = new OrderCreatedMessage(orderId, clientId, amount);

        orderCreatedConsumer.onOrderCreated(message);

        verify(processPaymentUseCase, times(1))
                .execute(orderId, clientId, amount);
    }
}
