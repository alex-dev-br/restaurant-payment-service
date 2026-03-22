package br.com.fiap.restaurant.payment.infra.messaging.inbound.consumer;

import br.com.fiap.restaurant.payment.core.usecase.HandleOrderCreatedEventUseCase;
import br.com.fiap.restaurant.payment.core.usecase.command.HandleOrderCreatedEventCommand;
import br.com.fiap.restaurant.payment.infra.messaging.dto.EventDTO;
import br.com.fiap.restaurant.payment.infra.messaging.inbound.dto.OrderDTO;
import br.com.fiap.restaurant.payment.infra.messaging.inbound.dto.OrderItemDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OrderCreatedConsumerTest {

    private HandleOrderCreatedEventUseCase handleOrderCreatedEventUseCase;
    private OrderCreatedConsumer consumer;

    @BeforeEach
    void setUp() {
        handleOrderCreatedEventUseCase = mock(HandleOrderCreatedEventUseCase.class);
        consumer = new OrderCreatedConsumer(handleOrderCreatedEventUseCase);
    }

    @Test
    void shouldDelegateOrderCreatedEventToHandleOrderCreatedEventUseCase() {
        UUID messageId = UUID.randomUUID();
        Long orderId = 1L;
        UUID clientId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("120.00");

        OrderDTO order = new OrderDTO(
                orderId,
                clientId,
                List.of(new OrderItemDTO(BigDecimal.ONE, amount))
        );

        EventDTO<OrderDTO> event = new EventDTO<>(
                messageId,
                "ORDER_CREATED",
                LocalDateTime.now(),
                order
        );

        consumer.onOrderCreated(event);

        ArgumentCaptor<HandleOrderCreatedEventCommand> captor =
                ArgumentCaptor.forClass(HandleOrderCreatedEventCommand.class);

        verify(handleOrderCreatedEventUseCase).execute(captor.capture());

        HandleOrderCreatedEventCommand command = captor.getValue();

        assertEquals(messageId, command.messageId());
        assertEquals(orderId, command.orderId());
        assertEquals(clientId, command.clientId());
        assertEquals(amount, command.amount());
    }
}
