package br.com.fiap.restaurant.payment.infra.messaging.e2e;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentOrderIdIdempotencyEndToEndIntegrationTest extends AbstractPaymentE2ETest {

    @BeforeEach
    void setUp() {
        resetState();
    }

    @AfterEach
    void tearDown() {
        resetState();
    }

    @Test
    void shouldProcessOnlyOnePaymentWhenTwoDifferentMessagesHaveSameOrderId() {
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

        sendOrderCreated(firstMessage);
        sendOrderCreated(secondMessage);

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

        Map<String, Object> outboxRow = awaitOutboxRowForOrderAndEventType(orderId, "PAYMENT_APPROVED");

        assertThat(outboxRow.get("event_type")).isEqualTo("PAYMENT_APPROVED");
        assertThat(outboxRow.get("routing_key")).isEqualTo("payment.approved");
        assertThat(outboxRow.get("status")).isEqualTo("PUBLISHED");
        assertThat(outboxRow.get("payload").toString()).contains("\"orderId\":" + orderId);

        String approvedPayload = awaitMessagePayload(paymentApprovedDebugQueue);

        assertThat(approvedPayload).contains("\"orderId\":" + orderId);
        assertThat(approvedPayload).contains("\"status\":\"APPROVED\"");

        assertQueueIsEmpty(paymentApprovedDebugQueue);
        assertQueueIsEmpty(paymentPendingDebugQueue);
        assertQueueIsEmpty(paymentFailedDebugQueue);
    }
}
