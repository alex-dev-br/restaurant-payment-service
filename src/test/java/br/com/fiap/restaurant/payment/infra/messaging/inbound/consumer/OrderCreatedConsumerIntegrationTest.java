package br.com.fiap.restaurant.payment.infra.messaging.inbound.consumer;

import br.com.fiap.restaurant.payment.core.usecase.HandleOrderCreatedEventUseCase;
import br.com.fiap.restaurant.payment.core.usecase.command.HandleOrderCreatedEventCommand;
import br.com.fiap.restaurant.payment.infra.messaging.config.RabbitProperties;
import br.com.fiap.restaurant.payment.infra.messaging.inbound.dto.OrderCreatedMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = {
        "spring.rabbitmq.listener.simple.auto-startup=true",
        "app.payment.retry.scheduler.enabled=false"
})
@ActiveProfiles("test")
class OrderCreatedConsumerIntegrationTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RabbitProperties rabbitProperties;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @MockitoBean
    private HandleOrderCreatedEventUseCase handleOrderCreatedEventUseCase;

    @BeforeEach
    void setUp() {
        assertNotNull(rabbitProperties.getExchange());
        assertNotNull(rabbitProperties.getRoutingKey());
        assertNotNull(rabbitProperties.getQueue());
        assertNotNull(rabbitProperties.getQueue().getPaymentOrderCreated());

        purgeQueue(rabbitProperties.getQueue().getPaymentOrderCreated());
    }

    @Test
    void shouldConsumeOrderCreatedMessageAndInvokeHandleOrderCreatedEventUseCase() {
        UUID messageId = UUID.randomUUID();
        Long orderId = 1L;
        UUID clientId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("120.00");

        OrderCreatedMessage message = new OrderCreatedMessage(
                messageId,
                orderId,
                clientId,
                amount
        );

        rabbitTemplate.convertAndSend(
                rabbitProperties.getExchange().getOrder(),
                rabbitProperties.getRoutingKey().getOrderCreated(),
                message
        );

        ArgumentCaptor<HandleOrderCreatedEventCommand> commandCaptor =
                ArgumentCaptor.forClass(HandleOrderCreatedEventCommand.class);

        verify(handleOrderCreatedEventUseCase, timeout(5000)).execute(commandCaptor.capture());

        HandleOrderCreatedEventCommand capturedCommand = commandCaptor.getValue();

        assertEquals(messageId, capturedCommand.messageId());
        assertEquals(orderId, capturedCommand.orderId());
        assertEquals(clientId, capturedCommand.clientId());
        assertEquals(amount, capturedCommand.amount());
    }

    private void purgeQueue(String queueName) {
        amqpAdmin.purgeQueue(queueName, true);
    }
}
