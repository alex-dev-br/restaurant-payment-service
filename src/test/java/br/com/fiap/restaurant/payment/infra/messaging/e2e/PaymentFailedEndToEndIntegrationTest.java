package br.com.fiap.restaurant.payment.infra.messaging.e2e;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentFailedEndToEndIntegrationTest extends AbstractPaymentE2ETest {

    @BeforeEach
    void setUp() {
        resetState();
    }

    @AfterEach
    void tearDown() {
        resetState();
    }

    @Test
    void shouldMarkPaymentAsFailedAfterMaxRetryAttemptsAndPublishFailedEvent() {
        processorWillReturnPending();
        processorWillReturnPending();
        processorWillReturnPending();

        TestOrderData orderData = newOrderData(new BigDecimal("180.00"));

        sendOrderCreated(orderData);

        awaitPaymentPersisted(orderData.orderId());
        awaitPaymentStatus(orderData.orderId(), "PENDING");
        awaitRetryCount(orderData.orderId(), 1);

        String initialPendingPayload = awaitMessagePayload(paymentPendingDebugQueue);
        assertThat(initialPendingPayload).contains("\"orderId\":" + orderData.orderId());
        assertThat(initialPendingPayload).contains("\"status\":\"PENDING\"");

        triggerRetryCycle();

        awaitPaymentStatus(orderData.orderId(), "PENDING");
        awaitRetryCount(orderData.orderId(), 2);

        triggerRetryCycle();

        awaitPaymentStatus(orderData.orderId(), "FAILED");
        awaitRetryCount(orderData.orderId(), 3);

        Map<String, Object> paymentRow = findPaymentRow(orderData.orderId());

        assertThat(((Number) paymentRow.get("order_id")).longValue()).isEqualTo(orderData.orderId());
        assertThat(paymentRow.get("client_id").toString()).isEqualTo(orderData.clientId().toString());
        assertThat(paymentRow.get("status")).isEqualTo("FAILED");
        assertThat(((Number) paymentRow.get("retry_count")).intValue()).isEqualTo(3);
        assertThat(paymentRow.get("last_retry_at")).isNotNull();
        assertThat(paymentRow.get("next_retry_at")).isNull();
        assertThat(new BigDecimal(paymentRow.get("amount").toString())).isEqualByComparingTo("180.00");

        assertThat(countProcessedMessages(orderData.messageId())).isEqualTo(1);

        Map<String, Object> outboxRow =
                awaitOutboxRowForOrderAndEventType(orderData.orderId(), "PAYMENT_FAILED");

        assertThat(outboxRow.get("event_type")).isEqualTo("PAYMENT_FAILED");
        assertThat(outboxRow.get("routing_key")).isEqualTo("payment.failed");
        assertThat(outboxRow.get("status")).isEqualTo("PUBLISHED");
        assertThat(outboxRow.get("payload").toString()).contains("\"orderId\":" + orderData.orderId());
        assertThat(outboxRow.get("payload").toString()).contains("\"status\":\"FAILED\"");

        String failedPayload = awaitMessagePayload(paymentFailedDebugQueue);

        assertThat(failedPayload).contains("\"orderId\":" + orderData.orderId());
        assertThat(failedPayload).contains("\"clientId\":\"" + orderData.clientId() + "\"");
        assertThat(failedPayload).contains("\"status\":\"FAILED\"");

        assertQueueIsEmpty(paymentApprovedDebugQueue);
        assertQueueIsEmpty(paymentPendingDebugQueue);
    }
}
