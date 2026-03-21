package br.com.fiap.restaurant.payment.infra.messaging.inbound.consumer;

import br.com.fiap.restaurant.payment.core.gateway.ProcessedMessageRepositoryGateway;
import br.com.fiap.restaurant.payment.core.usecase.ProcessPaymentUseCase;
import br.com.fiap.restaurant.payment.core.usecase.command.ProcessPaymentCommand;
import br.com.fiap.restaurant.payment.infra.messaging.config.RabbitProperties;
import br.com.fiap.restaurant.payment.infra.messaging.inbound.dto.OrderCreatedMessage;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "spring.rabbitmq.listener.simple.auto-startup=true",
        "app.external-payment.fake-enabled=true",
        "app.payment.retry.scheduler.enabled=false",
        "app.payment.outbox.publisher.enabled=false"
})
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class OrderCreatedConsumerIntegrationTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RabbitProperties rabbitProperties;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @MockitoBean
    private ProcessPaymentUseCase processPaymentUseCase;

    @MockitoBean
    private ProcessedMessageRepositoryGateway processedMessageRepositoryGateway;

    @BeforeEach
    void setUp() {
        reset(processPaymentUseCase, processedMessageRepositoryGateway);

        when(processedMessageRepositoryGateway.registerIfAbsent(
                org.mockito.ArgumentMatchers.any(UUID.class),
                org.mockito.ArgumentMatchers.eq("ORDER_CREATED"),
                org.mockito.ArgumentMatchers.anyString()
        )).thenReturn(true);

        assertNotNull(rabbitProperties.getExchange());
        assertNotNull(rabbitProperties.getRoutingKey());
        assertNotNull(rabbitProperties.getQueue());
        assertNotNull(rabbitProperties.getQueue().getPaymentOrderCreated());

        purgeQueue(rabbitProperties.getQueue().getPaymentOrderCreated());
    }

    @Test
    void shouldConsumeOrderCreatedMessageAndInvokeProcessPaymentUseCase() {
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

        ArgumentCaptor<ProcessPaymentCommand> commandCaptor =
                ArgumentCaptor.forClass(ProcessPaymentCommand.class);

        verify(processPaymentUseCase, timeout(10000)).execute(commandCaptor.capture());

        ProcessPaymentCommand capturedCommand = commandCaptor.getValue();

        assertEquals(orderId, capturedCommand.orderId());
        assertEquals(clientId, capturedCommand.clientId());
        assertEquals(amount, capturedCommand.amount());
    }

    private void purgeQueue(String queueName) {
        amqpAdmin.purgeQueue(queueName, true);
    }
}
