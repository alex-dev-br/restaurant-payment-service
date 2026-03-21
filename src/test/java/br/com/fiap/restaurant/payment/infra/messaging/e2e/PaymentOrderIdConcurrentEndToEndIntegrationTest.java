package br.com.fiap.restaurant.payment.infra.messaging.e2e;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {
        "spring.rabbitmq.listener.simple.concurrency=2",
        "spring.rabbitmq.listener.simple.max-concurrency=2",
        "spring.rabbitmq.listener.simple.prefetch=1"
})
class PaymentOrderIdConcurrentEndToEndIntegrationTest extends AbstractPaymentE2ETest {

    @BeforeEach
    void setUp() {
        resetState();
    }

    @AfterEach
    void tearDown() {
        resetState();
    }

    @Test
    void shouldKeepSinglePaymentWhenTwoDifferentMessagesForSameOrderIdArriveConcurrently() throws Exception {
        long orderId = 987654L;
        UUID clientId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("120.00");

        TestOrderData firstMessage = new TestOrderData(
                UUID.randomUUID(),
                orderId,
                clientId,
                amount
        );

        TestOrderData secondMessage = new TestOrderData(
                UUID.randomUUID(),
                orderId,
                clientId,
                amount
        );

        processorWillApprove();
        processorWillApprove();

        publishConcurrently(firstMessage, secondMessage);

        awaitPaymentPersisted(orderId);
        awaitPaymentStatus(orderId, "APPROVED");

        Awaitility.await()
                .atMost(DEFAULT_TIMEOUT)
                .pollInterval(DEFAULT_POLL_INTERVAL)
                .untilAsserted(() -> {
                    Integer paymentsCount = jdbcTemplate.queryForObject(
                            "select count(*) from payments where order_id = ?",
                            Integer.class,
                            orderId
                    );

                    Integer outboxCount = jdbcTemplate.queryForObject(
                            """
                            select count(*)
                            from payment_outbox
                            where payload like ?
                              and event_type = 'PAYMENT_APPROVED'
                            """,
                            Integer.class,
                            "%" + orderId + "%"
                    );

                    Integer processedMessagesCount = jdbcTemplate.queryForObject(
                            "select count(*) from processed_messages",
                            Integer.class
                    );

                    assertThat(paymentsCount).isEqualTo(1);
                    assertThat(outboxCount).isEqualTo(1);
                    assertThat(processedMessagesCount).isEqualTo(2);
                });

        Map<String, Object> paymentRow = findPaymentRow(orderId);

        assertThat(((Number) paymentRow.get("order_id")).longValue()).isEqualTo(orderId);
        assertThat(paymentRow.get("client_id").toString()).isEqualTo(clientId.toString());
        assertThat(paymentRow.get("status")).isEqualTo("APPROVED");
        assertThat(((Number) paymentRow.get("retry_count")).intValue()).isZero();
        assertThat(new BigDecimal(paymentRow.get("amount").toString()))
                .isEqualByComparingTo("120.00");

        assertThat(countProcessedMessages(firstMessage.messageId())).isEqualTo(1);
        assertThat(countProcessedMessages(secondMessage.messageId())).isEqualTo(1);

        Map<String, Object> outboxRow =
                awaitOutboxRowForOrderAndEventType(orderId, "PAYMENT_APPROVED");

        assertThat(outboxRow.get("event_type")).isEqualTo("PAYMENT_APPROVED");
        assertThat(outboxRow.get("routing_key")).isEqualTo("payment.approved");
        assertThat(outboxRow.get("status")).isEqualTo("PUBLISHED");
        assertThat(outboxRow.get("payload").toString()).contains("\"orderId\":" + orderId);
        assertThat(outboxRow.get("payload").toString()).contains("\"status\":\"APPROVED\"");

        String approvedPayload = awaitMessagePayload(paymentApprovedDebugQueue);

        assertThat(approvedPayload).contains("\"orderId\":" + orderId);
        assertThat(approvedPayload).contains("\"clientId\":\"" + clientId + "\"");
        assertThat(approvedPayload).contains("\"status\":\"APPROVED\"");

        assertQueueIsEmpty(paymentApprovedDebugQueue);
        assertQueueIsEmpty(paymentPendingDebugQueue);
        assertQueueIsEmpty(paymentFailedDebugQueue);
    }

    private void publishConcurrently(TestOrderData... messages) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(messages.length);
        CountDownLatch ready = new CountDownLatch(messages.length);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        try {
            for (TestOrderData message : messages) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
                    sendOrderCreated(message);
                    return null;
                }));
            }

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            for (Future<?> future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }
    }
}
