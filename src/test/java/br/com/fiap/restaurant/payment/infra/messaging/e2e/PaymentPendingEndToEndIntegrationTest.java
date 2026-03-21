package br.com.fiap.restaurant.payment.infra.messaging.e2e;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentPendingEndToEndIntegrationTest extends AbstractPaymentE2ETest {

    @BeforeEach
    void setUp() {
        resetState();
    }

    @AfterEach
    void tearDown() {
        resetState();
    }

    @Test
    void shouldConsumeOrderCreatedAndPublishPendingEventEndToEnd() {
        processorWillReturnPending();

        TestOrderData orderData = newOrderData(new BigDecimal("150.00"));

        sendOrderCreated(orderData);

        awaitPaymentPersisted(orderData.orderId());
        awaitPaymentStatus(orderData.orderId(), "PENDING");
        awaitRetryCount(orderData.orderId(), 1);

        Map<String, Object> paymentRow = findPaymentRow(orderData.orderId());

        assertThat(((Number) paymentRow.get("order_id")).longValue()).isEqualTo(orderData.orderId());
        assertThat(paymentRow.get("client_id").toString()).isEqualTo(orderData.clientId().toString());
        assertThat(paymentRow.get("status")).isEqualTo("PENDING");
        assertThat(((Number) paymentRow.get("retry_count")).intValue()).isEqualTo(1);
        assertThat(paymentRow.get("last_retry_at")).isNotNull();
        assertThat(paymentRow.get("next_retry_at")).isNotNull();
        assertThat(new BigDecimal(paymentRow.get("amount").toString())).isEqualByComparingTo("150.00");

        assertThat(countProcessedMessages(orderData.messageId())).isEqualTo(1);

        Map<String, Object> outboxRow =
                awaitOutboxRowForOrderAndEventType(orderData.orderId(), "PAYMENT_PENDING");

        assertThat(outboxRow.get("event_type")).isEqualTo("PAYMENT_PENDING");
        assertThat(outboxRow.get("routing_key")).isEqualTo("payment.pending");
        assertThat(outboxRow.get("status")).isEqualTo("PUBLISHED");
        assertThat(outboxRow.get("payload").toString()).contains("\"orderId\":" + orderData.orderId());
        assertThat(outboxRow.get("payload").toString()).contains("\"status\":\"PENDING\"");

        String pendingPayload = awaitMessagePayload(paymentPendingDebugQueue);

        assertThat(pendingPayload).contains("\"orderId\":" + orderData.orderId());
        assertThat(pendingPayload).contains("\"clientId\":\"" + orderData.clientId() + "\"");
        assertThat(pendingPayload).contains("\"status\":\"PENDING\"");

        assertQueueIsEmpty(paymentApprovedDebugQueue);
        assertQueueIsEmpty(paymentFailedDebugQueue);
    }
}
