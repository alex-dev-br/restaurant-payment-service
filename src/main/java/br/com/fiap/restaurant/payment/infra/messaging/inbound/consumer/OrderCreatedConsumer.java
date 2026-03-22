package br.com.fiap.restaurant.payment.infra.messaging.inbound.consumer;

import br.com.fiap.restaurant.payment.core.usecase.HandleOrderCreatedEventUseCase;
import br.com.fiap.restaurant.payment.core.usecase.command.HandleOrderCreatedEventCommand;
import br.com.fiap.restaurant.payment.infra.messaging.dto.EventDTO;
import br.com.fiap.restaurant.payment.infra.messaging.inbound.dto.OrderDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedConsumer.class);

    private final HandleOrderCreatedEventUseCase handleOrderCreatedEventUseCase;

    public OrderCreatedConsumer(HandleOrderCreatedEventUseCase handleOrderCreatedEventUseCase) {
        this.handleOrderCreatedEventUseCase = handleOrderCreatedEventUseCase;
    }

    @RabbitListener(queues = "${app.rabbit.queue.payment-order-created}")
    public void onOrderCreated(EventDTO<OrderDTO> message) {
        log.info("Consuming create order event: {}", message);
        OrderDTO orderDTO = message.body();
        HandleOrderCreatedEventCommand command = new HandleOrderCreatedEventCommand(
                message.uuid(),
                orderDTO.id(),
                orderDTO.customerUuid(),
                orderDTO.total()
        );

        handleOrderCreatedEventUseCase.execute(command);
    }
}