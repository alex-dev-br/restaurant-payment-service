package br.com.fiap.restaurant.payment.infra.messaging.e2e;

import br.com.fiap.restaurant.payment.core.usecase.RetryPendingPaymentsUseCase;
import br.com.fiap.restaurant.payment.infra.messaging.dto.EventDTO;
import br.com.fiap.restaurant.payment.infra.messaging.inbound.dto.OrderDTO;
import br.com.fiap.restaurant.payment.infra.messaging.inbound.dto.OrderItemDTO;
import br.com.fiap.restaurant.payment.support.AbstractMessagingIntegrationTest;
import org.awaitility.Awaitility;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.rabbitmq.listener.simple.auto-startup=true",
                "app.payment.retry.scheduler.enabled=false",
                "app.payment.retry.scheduler.fixed-delay-ms=0",
                "app.payment.outbox.publisher.enabled=true",
                "app.payment.outbox.publisher.fixed-delay-ms=500",
                "spring.jpa.show-sql=false"
        }
)
@ActiveProfiles("test")
@AutoConfigureTestRestTemplate
@Import(ControlledExternalPaymentProcessorTestConfig.class)
public abstract class AbstractPaymentE2ETest extends AbstractMessagingIntegrationTest {

    protected static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(15);
    protected static final Duration DEFAULT_POLL_INTERVAL = Duration.ofMillis(250);

    @Value("${app.rabbit.exchange.order}")
    protected String orderExchange;

    @Value("${app.rabbit.routing-key.order-created}")
    protected String orderCreatedRoutingKey;

    @Value("${app.rabbit.queue.payment-order-created}")
    protected String paymentOrderCreatedQueue;

    @Value("${app.rabbit.queue.payment-approved-debug}")
    protected String paymentApprovedDebugQueue;

    @Value("${app.rabbit.queue.payment-pending-debug}")
    protected String paymentPendingDebugQueue;

    @Value("${app.rabbit.queue.payment-failed-debug}")
    protected String paymentFailedDebugQueue;

    @Value("${app.payment.retry.policy.max-attempts}")
    protected int maxRetryAttempts;

    @Autowired
    protected RabbitTemplate rabbitTemplate;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected RetryPendingPaymentsUseCase retryPendingPaymentsUseCase;

    @Autowired
    protected ControlledExternalPaymentProcessorTestConfig.ControlledExternalPaymentProcessorClient
            controlledExternalPaymentProcessorClient;

    protected TestOrderData newOrderData(BigDecimal amount) {
        return new TestOrderData(
                UUID.randomUUID(),
                ThreadLocalRandom.current().nextLong(100_000, 999_999),
                UUID.randomUUID(),
                amount
        );
    }

    protected EventDTO<OrderDTO> buildOrderCreatedMessage(TestOrderData orderData) {
        OrderDTO body = new OrderDTO(
                orderData.orderId(),
                orderData.clientId(),
                List.of(new OrderItemDTO(BigDecimal.ONE, orderData.amount()))
        );

        return new EventDTO<>(
                orderData.messageId(),
                "ORDER_CREATED",
                LocalDateTime.now(),
                body
        );
    }

    protected void sendOrderCreated(TestOrderData orderData) {
        rabbitTemplate.convertAndSend(
                orderExchange,
                orderCreatedRoutingKey,
                buildOrderCreatedMessage(orderData)
        );
    }

    protected void processorWillApprove() {
        controlledExternalPaymentProcessorClient.enqueueApproved();
    }

    protected void processorWillReturnPending() {
        controlledExternalPaymentProcessorClient.enqueuePending();
    }

    protected void processorWillThrow(String message) {
        controlledExternalPaymentProcessorClient.enqueueFailure(new RuntimeException(message));
    }

    protected void triggerRetryCycle() {
        retryPendingPaymentsUseCase.execute();
    }

    protected void awaitPaymentPersisted(long orderId) {
        Awaitility.await()
                .atMost(DEFAULT_TIMEOUT)
                .pollInterval(DEFAULT_POLL_INTERVAL)
                .untilAsserted(() -> {
                    Integer paymentsCount = jdbcTemplate.queryForObject(
                            "select count(*) from payments where order_id = ?",
                            Integer.class,
                            orderId
                    );
                    org.assertj.core.api.Assertions.assertThat(paymentsCount).isEqualTo(1);
                });
    }

    protected void awaitPaymentStatus(long orderId, String expectedStatus) {
        Awaitility.await()
                .atMost(DEFAULT_TIMEOUT)
                .pollInterval(DEFAULT_POLL_INTERVAL)
                .untilAsserted(() -> {
                    String currentStatus = jdbcTemplate.queryForObject(
                            "select status from payments where order_id = ?",
                            String.class,
                            orderId
                    );
                    org.assertj.core.api.Assertions.assertThat(currentStatus).isEqualTo(expectedStatus);
                });
    }

    protected void awaitRetryCount(long orderId, int expectedRetryCount) {
        Awaitility.await()
                .atMost(DEFAULT_TIMEOUT)
                .pollInterval(DEFAULT_POLL_INTERVAL)
                .untilAsserted(() -> {
                    Integer currentRetryCount = jdbcTemplate.queryForObject(
                            "select retry_count from payments where order_id = ?",
                            Integer.class,
                            orderId
                    );
                    org.assertj.core.api.Assertions.assertThat(currentRetryCount)
                            .isEqualTo(expectedRetryCount);
                });
    }

    protected Map<String, Object> findPaymentRow(long orderId) {
        return jdbcTemplate.queryForMap("""
                select
                    id,
                    order_id,
                    client_id,
                    status,
                    retry_count,
                    last_retry_at,
                    next_retry_at,
                    amount::text as amount
                from payments
                where order_id = ?
                """, orderId);
    }

    protected int countProcessedMessages(UUID messageId) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*)
                from processed_messages
                where message_id = ?
                """, Integer.class, messageId);

        return count == null ? 0 : count;
    }

    protected Map<String, Object> awaitLatestOutboxRowForOrder(long orderId) {
        final Map<String, Object>[] holder = new Map[1];

        Awaitility.await()
                .atMost(DEFAULT_TIMEOUT)
                .pollInterval(DEFAULT_POLL_INTERVAL)
                .untilAsserted(() -> {
                    holder[0] = jdbcTemplate.queryForMap("""
                            select
                                id,
                                aggregate_id,
                                event_type,
                                exchange_name,
                                routing_key,
                                status,
                                payload,
                                created_at,
                                published_at
                            from payment_outbox
                            where payload like ?
                            order by created_at desc
                            fetch first 1 row only
                            """, "%" + orderId + "%");

                    org.assertj.core.api.Assertions.assertThat(holder[0]).isNotNull();
                });

        return holder[0];
    }

    protected Map<String, Object> awaitOutboxRowForOrderAndEventType(long orderId, String eventType) {
        final Map<String, Object>[] holder = new Map[1];

        Awaitility.await()
                .atMost(DEFAULT_TIMEOUT)
                .pollInterval(DEFAULT_POLL_INTERVAL)
                .untilAsserted(() -> {
                    holder[0] = jdbcTemplate.queryForMap("""
                            select
                                id,
                                aggregate_id,
                                event_type,
                                exchange_name,
                                routing_key,
                                status,
                                payload,
                                created_at,
                                published_at
                            from payment_outbox
                            where payload like ?
                              and event_type = ?
                            order by created_at desc
                            fetch first 1 row only
                            """, "%" + orderId + "%", eventType);

                    org.assertj.core.api.Assertions.assertThat(holder[0]).isNotNull();
                });

        return holder[0];
    }

    protected Message awaitMessage(String queueName) {
        final Message[] holder = new Message[1];

        Awaitility.await()
                .atMost(DEFAULT_TIMEOUT)
                .pollInterval(DEFAULT_POLL_INTERVAL)
                .untilAsserted(() -> {
                    holder[0] = rabbitTemplate.receive(queueName, 1000);
                    org.assertj.core.api.Assertions.assertThat(holder[0]).isNotNull();
                });

        return holder[0];
    }

    protected String awaitMessagePayload(String queueName) {
        Message message = awaitMessage(queueName);
        return new String(message.getBody(), StandardCharsets.UTF_8);
    }

    protected Message receiveOrNull(String queueName) {
        return rabbitTemplate.receive(queueName, 500);
    }

    protected void assertQueueIsEmpty(String queueName) {
        org.assertj.core.api.Assertions.assertThat(receiveOrNull(queueName)).isNull();
    }

    protected void cleanDatabase() {
        jdbcTemplate.execute("delete from payment_outbox");
        jdbcTemplate.execute("delete from processed_messages");
        jdbcTemplate.execute("delete from payments");
    }

    protected void purgeQueues() {
        rabbitTemplate.execute(channel -> {
            channel.queuePurge(paymentOrderCreatedQueue);
            channel.queuePurge(paymentApprovedDebugQueue);
            channel.queuePurge(paymentPendingDebugQueue);
            channel.queuePurge(paymentFailedDebugQueue);
            return null;
        });
    }

    protected void resetState() {
        controlledExternalPaymentProcessorClient.reset();
        purgeQueues();
        cleanDatabase();
    }

    protected record TestOrderData(
            UUID messageId,
            long orderId,
            UUID clientId,
            BigDecimal amount
    ) {
    }
}
