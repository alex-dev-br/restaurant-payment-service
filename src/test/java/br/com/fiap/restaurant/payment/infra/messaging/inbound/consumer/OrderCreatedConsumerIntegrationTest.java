package br.com.fiap.restaurant.payment.infra.messaging.inbound.consumer;

import br.com.fiap.restaurant.payment.core.usecase.HandleOrderCreatedEventUseCase;
import br.com.fiap.restaurant.payment.core.usecase.command.HandleOrderCreatedEventCommand;
import br.com.fiap.restaurant.payment.infra.messaging.config.RabbitProperties;
import br.com.fiap.restaurant.payment.infra.messaging.dto.EventDTO;
import br.com.fiap.restaurant.payment.infra.messaging.inbound.dto.OrderDTO;
import br.com.fiap.restaurant.payment.infra.messaging.inbound.dto.OrderItemDTO;
import br.com.fiap.restaurant.payment.support.AbstractMessagingIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = {
        "spring.rabbitmq.listener.simple.auto-startup=true",
        "app.external-payment.fake-enabled=true",
        "app.payment.retry.scheduler.enabled=false",
        "app.payment.outbox.publisher.enabled=false"
})
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class OrderCreatedConsumerIntegrationTest extends AbstractMessagingIntegrationTest {

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
        reset(handleOrderCreatedEventUseCase);
        purgeQueue(rabbitProperties.getQueue().getPaymentOrderCreated());
    }

    @Test
    void shouldConsumeOrderCreatedEventAndInvokeHandleOrderCreatedEventUseCase() {
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

        rabbitTemplate.convertAndSend(
                rabbitProperties.getExchange().getOrder(),
                rabbitProperties.getRoutingKey().getOrderCreated(),
                event
        );

        ArgumentCaptor<HandleOrderCreatedEventCommand> commandCaptor =
                ArgumentCaptor.forClass(HandleOrderCreatedEventCommand.class);

        verify(handleOrderCreatedEventUseCase, timeout(10000)).execute(commandCaptor.capture());

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
