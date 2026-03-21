package br.com.fiap.restaurant.payment.infra.messaging.e2e;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentApprovedEndToEndIntegrationTest extends AbstractPaymentE2ETest {

    @BeforeEach
    void setUp() {
        resetState();
    }

    @AfterEach
    void tearDown() {
        resetState();
    }

    @Test
    void shouldConsumeOrderCreatedAndPublishApprovedEventEndToEnd() {
        TestOrderData orderData = newOrderData(new BigDecimal("120.00"));

        sendOrderCreated(orderData);

        awaitPaymentPersisted(orderData.orderId());

        Map<String, Object> paymentRow = findPaymentRow(orderData.orderId());

        assertThat(((Number) paymentRow.get("order_id")).longValue()).isEqualTo(orderData.orderId());
        assertThat(paymentRow.get("client_id").toString()).isEqualTo(orderData.clientId().toString());
        assertThat(paymentRow.get("status")).isEqualTo("APPROVED");
        assertThat(((Number) paymentRow.get("retry_count")).intValue()).isZero();
        assertThat(new BigDecimal(paymentRow.get("amount").toString())).isEqualByComparingTo("120.00");

        assertThat(countProcessedMessages(orderData.messageId())).isEqualTo(1);

        Map<String, Object> outboxRow = awaitLatestOutboxRowForOrder(orderData.orderId());

        assertThat(outboxRow.get("event_type")).isEqualTo("PAYMENT_APPROVED");
        assertThat(outboxRow.get("routing_key")).isEqualTo("payment.approved");
        assertThat(outboxRow.get("status")).isEqualTo("PUBLISHED");
        assertThat(outboxRow.get("payload").toString()).contains("\"orderId\":" + orderData.orderId());
        assertThat(outboxRow.get("payload").toString()).contains("\"status\":\"APPROVED\"");

        String approvedPayload = awaitMessagePayload(paymentApprovedDebugQueue);

        assertThat(approvedPayload).contains("\"orderId\":" + orderData.orderId());
        assertThat(approvedPayload).contains("\"clientId\":\"" + orderData.clientId() + "\"");
        assertThat(approvedPayload).contains("\"status\":\"APPROVED\"");

        assertQueueIsEmpty(paymentPendingDebugQueue);
        assertQueueIsEmpty(paymentFailedDebugQueue);
    }
}
