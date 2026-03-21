package br.com.fiap.restaurant.payment.infra.messaging.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import br.com.fiap.restaurant.payment.infra.messaging.inbound.dto.OrderCreatedMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "spring.rabbitmq.listener.simple.auto-startup=true",
        "app.external-payment.fake-enabled=true",
        "app.payment.retry.scheduler.enabled=false",
        "app.payment.outbox.publisher.enabled=true",
        "app.payment.outbox.publisher.fixed-delay-ms=500",
        "spring.jpa.show-sql=false"
})
@ActiveProfiles("test")
class PaymentApprovedEndToEndIntegrationTest {

    @Value("${app.rabbit.exchange.order}")
    private String orderExchange;

    @Value("${app.rabbit.routing-key.order-created}")
    private String orderCreatedRoutingKey;

    @Value("${app.rabbit.queue.payment-order-created}")
    private String paymentOrderCreatedQueue;

    @Value("${app.rabbit.queue.payment-approved-debug}")
    private String paymentApprovedDebugQueue;

    @Value("${app.rabbit.queue.payment-pending-debug}")
    private String paymentPendingDebugQueue;

    @Value("${app.rabbit.queue.payment-failed-debug}")
    private String paymentFailedDebugQueue;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        purgeQueues();
        cleanDatabase();
    }

    @AfterEach
    void tearDown() {
        purgeQueues();
        cleanDatabase();
    }

    @Test
    void shouldConsumeOrderCreatedAndPublishApprovedEventEndToEnd() {
        UUID messageId = UUID.randomUUID();
        long orderId = ThreadLocalRandom.current().nextLong(100_000, 999_999);
        UUID clientId = UUID.randomUUID();

        OrderCreatedMessage message = new OrderCreatedMessage(
                messageId,
                orderId,
                clientId,
                new BigDecimal("120.00")
        );

        rabbitTemplate.convertAndSend(
                orderExchange,
                orderCreatedRoutingKey,
                message
        );

        await()
                .atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(250))
                .untilAsserted(() -> {
                    Integer paymentsCount = jdbcTemplate.queryForObject(
                            "select count(*) from payments where order_id = ?",
                            Integer.class,
                            orderId
                    );
                    assertThat(paymentsCount).isEqualTo(1);
                });

        Map<String, Object> paymentRow = jdbcTemplate.queryForMap("""
                select
                    id,
                    order_id,
                    client_id,
                    status,
                    retry_count,
                    amount::text as amount
                from payments
                where order_id = ?
                """, orderId);

        assertThat(((Number) paymentRow.get("order_id")).longValue()).isEqualTo(orderId);
        assertThat(paymentRow.get("client_id").toString()).isEqualTo(clientId.toString());
        assertThat(paymentRow.get("status")).isEqualTo("APPROVED");
        assertThat(((Number) paymentRow.get("retry_count")).intValue()).isZero();
        assertThat(new BigDecimal(paymentRow.get("amount").toString())).isEqualByComparingTo("120.00");

        Integer processedMessagesCount = jdbcTemplate.queryForObject("""
                select count(*)
                from processed_messages
                where message_id = ?
                """, Integer.class, messageId);

        assertThat(processedMessagesCount).isEqualTo(1);

        await()
                .atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(250))
                .untilAsserted(() -> {
                    Map<String, Object> outboxRow = jdbcTemplate.queryForMap("""
                            select
                                event_type,
                                routing_key,
                                status,
                                payload
                            from payment_outbox
                            where payload like ?
                            order by created_at desc
                            fetch first 1 row only
                            """, "%" + orderId + "%");

                    assertThat(outboxRow.get("event_type")).isEqualTo("PAYMENT_APPROVED");
                    assertThat(outboxRow.get("routing_key")).isEqualTo("payment.approved");
                    assertThat(outboxRow.get("status")).isEqualTo("PUBLISHED");
                    assertThat(outboxRow.get("payload").toString()).contains("\"orderId\":" + orderId);
                    assertThat(outboxRow.get("payload").toString()).contains("\"status\":\"APPROVED\"");
                });

        Message approvedMessage = awaitApprovedMessage();
        String approvedPayload = new String(approvedMessage.getBody(), StandardCharsets.UTF_8);

        assertThat(approvedPayload).contains("\"orderId\":" + orderId);
        assertThat(approvedPayload).contains("\"clientId\":\"" + clientId + "\"");
        assertThat(approvedPayload).contains("\"status\":\"APPROVED\"");

        Message pendingMessage = rabbitTemplate.receive(paymentPendingDebugQueue, 500);
        Message failedMessage = rabbitTemplate.receive(paymentFailedDebugQueue, 500);

        assertThat(pendingMessage).isNull();
        assertThat(failedMessage).isNull();
    }

    private Message awaitApprovedMessage() {
        final Message[] holder = new Message[1];

        await()
                .atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(250))
                .untilAsserted(() -> {
                    holder[0] = rabbitTemplate.receive(paymentApprovedDebugQueue, 1000);
                    assertThat(holder[0]).isNotNull();
                });

        return holder[0];
    }

    private void cleanDatabase() {
        jdbcTemplate.execute("delete from payment_outbox");
        jdbcTemplate.execute("delete from processed_messages");
        jdbcTemplate.execute("delete from payments");
    }

    private void purgeQueues() {
        rabbitTemplate.execute(channel -> {
            channel.queuePurge(paymentOrderCreatedQueue);
            channel.queuePurge(paymentApprovedDebugQueue);
            channel.queuePurge(paymentPendingDebugQueue);
            channel.queuePurge(paymentFailedDebugQueue);
            return null;
        });
    }
}