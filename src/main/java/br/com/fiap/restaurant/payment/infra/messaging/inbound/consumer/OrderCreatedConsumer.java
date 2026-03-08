package br.com.fiap.restaurant.payment.infra.messaging.inbound.consumer;

import br.com.fiap.restaurant.payment.core.usecase.ProcessPaymentUseCase;
import br.com.fiap.restaurant.payment.infra.messaging.inbound.dto.OrderCreatedMessage;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedConsumer {

    private final ProcessPaymentUseCase processPaymentUseCase;

    public OrderCreatedConsumer(ProcessPaymentUseCase processPaymentUseCase) {
        this.processPaymentUseCase = processPaymentUseCase;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.order-created}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void onOrderCreated(OrderCreatedMessage message) {
        processPaymentUseCase.execute(
                message.orderId(),
                message.clientId(),
                message.amount()
        );
    }
}
