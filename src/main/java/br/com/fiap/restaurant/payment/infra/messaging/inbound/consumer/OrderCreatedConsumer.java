package br.com.fiap.restaurant.payment.infra.messaging.inbound.consumer;

import br.com.fiap.restaurant.payment.core.usecase.HandleOrderCreatedEventUseCase;
import br.com.fiap.restaurant.payment.core.usecase.command.HandleOrderCreatedEventCommand;
import br.com.fiap.restaurant.payment.infra.messaging.inbound.dto.OrderCreatedMessage;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedConsumer {

    private final HandleOrderCreatedEventUseCase handleOrderCreatedEventUseCase;

    public OrderCreatedConsumer(HandleOrderCreatedEventUseCase handleOrderCreatedEventUseCase) {
        this.handleOrderCreatedEventUseCase = handleOrderCreatedEventUseCase;
    }

    @RabbitListener(queues = "${app.rabbit.queue.payment-order-created}")
    public void onOrderCreated(OrderCreatedMessage message) {
        HandleOrderCreatedEventCommand command = new HandleOrderCreatedEventCommand(
                message.messageId(),
                message.orderId(),
                message.clientId(),
                message.amount()
        );

        handleOrderCreatedEventUseCase.execute(command);
    }
}