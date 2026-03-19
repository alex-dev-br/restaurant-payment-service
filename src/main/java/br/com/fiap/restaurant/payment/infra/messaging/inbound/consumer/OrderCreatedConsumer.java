package br.com.fiap.restaurant.payment.infra.messaging.inbound.consumer;

import br.com.fiap.restaurant.payment.core.usecase.ProcessPaymentUseCase;
import br.com.fiap.restaurant.payment.core.usecase.command.ProcessPaymentCommand;
import br.com.fiap.restaurant.payment.infra.messaging.inbound.dto.OrderCreatedMessage;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedConsumer {

    private final ProcessPaymentUseCase processPaymentUseCase;

    public OrderCreatedConsumer(ProcessPaymentUseCase processPaymentUseCase) {
        this.processPaymentUseCase = processPaymentUseCase;
    }

    @RabbitListener(queues = "${app.rabbit.queue.payment-order-created}")
    public void onOrderCreated(OrderCreatedMessage message) {
        ProcessPaymentCommand command = new ProcessPaymentCommand(
                message.orderId(),
                message.clientId(),
                message.amount()
        );

        processPaymentUseCase.execute(command);
    }
}